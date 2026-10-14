"""Pruebas HTTP de inventario contra una instancia MySQL temporal ya iniciada.

Requiere el backend apuntando a esa instancia y el esquema maestro cargado.
Se niega a escribir si el puerto o el directorio de datos no coinciden con
la instancia temporal indicada por quien ejecuta la prueba.
"""

import argparse
import json
import os
import subprocess
import threading
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen


TEST_PASSWORD = "IntiWasiTest123!"
TEST_PASSWORD_HASH = "$2b$12$qc6SKNp6c5dC7I0KnOVTIePq1GSWOQP2.xo8g/EAdoS6wB4rpo0fW"


def check(condition, message):
    if not condition:
        raise AssertionError(message)


class IsolatedRun:
    def __init__(self, args):
        self.args = args
        self.mysql_args = [
            args.mysql_bin,
            "--no-defaults",
            "--protocol=TCP",
            "--host=127.0.0.1",
            f"--port={args.mysql_port}",
            f"--user={args.mysql_user}",
            "--default-character-set=utf8mb4",
            "--batch",
            "--raw",
            "--skip-column-names",
            "--database=bd_intiwasi",
        ]
        self.token = None
        self.suffix = uuid.uuid4().hex[:12]

    def sql(self, statement):
        env = os.environ.copy()
        if self.args.mysql_password:
            env["MYSQL_PWD"] = self.args.mysql_password
        result = subprocess.run(
            self.mysql_args,
            input=statement,
            text=True,
            encoding="utf-8",
            capture_output=True,
            check=True,
            env=env,
        )
        return result.stdout.strip().splitlines()

    def verify_database(self):
        check(self.args.mysql_port != 3306, "El puerto 3306 no se admite para estas pruebas.")
        port, datadir = self.sql("SELECT @@port, @@datadir")[0].split("\t")
        expected = os.path.normcase(os.path.normpath(self.args.expected_datadir))
        actual = os.path.normcase(os.path.normpath(datadir))
        check(int(port) == self.args.mysql_port and actual == expected, "La instancia MySQL no coincide con la indicada.")
        check(Path(actual).name.startswith("intiwasi-mysql-ux-"), "El directorio de datos no parece temporal para Inti Wasi.")
        tables = int(self.sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='bd_intiwasi'")[0])
        triggers = int(self.sql("SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema='bd_intiwasi'")[0])
        check(tables >= 9 and triggers >= 10, "Faltan tablas o triggers maestros en la base aislada.")

    def api(self, method, path, payload=None, token=None):
        headers = {"Content-Type": "application/json"}
        bearer = token if token is not None else self.token
        if bearer:
            headers["Authorization"] = f"Bearer {bearer}"
        body = json.dumps(payload).encode("utf-8") if payload is not None else None
        request = Request(self.args.api_url + path, body, headers, method=method)
        try:
            with urlopen(request, timeout=30) as response:
                raw = response.read()
                return response.status, json.loads(raw) if raw else None
        except HTTPError as error:
            raw = error.read()
            return error.code, json.loads(raw) if raw else None

    def require(self, method, path, payload=None, status=200, token=None):
        actual, result = self.api(method, path, payload, token)
        check(actual == status, f"{method} {path}: esperado {status}, recibido {actual}: {result}")
        return result

    def fixture(self):
        suffix = self.suffix
        admin_email = f"ux-admin-{suffix}@intiwasi.pe"
        operator_email = f"ux-operador-{suffix}@intiwasi.pe"
        number = int(suffix[:10], 16) % 10_000_000_000
        ruc = f"2{number:010d}"
        phone = f"9{number % 100_000_000:08d}"
        self.sql(f"""
            INSERT INTO Usuarios (NomUsuario, Correo, Contrasena, Rol, Telefono, Estado) VALUES
            ('Administradora de prueba', '{admin_email}', '{TEST_PASSWORD_HASH}', 'Administrador', '{phone}', 1),
            ('Operador de prueba', '{operator_email}', '{TEST_PASSWORD_HASH}', 'Operador de Almacén', NULL, 1);
            INSERT INTO Categorias (NomCategoria) VALUES ('Categoría de prueba {suffix}');
            INSERT INTO Proveedores (NomProveedor, RUC, Telefono) VALUES ('Proveedor de prueba {suffix}', '{ruc}', '900000000');
        """)
        category = int(self.sql(f"SELECT IdCategoria FROM Categorias WHERE NomCategoria='Categoría de prueba {suffix}'")[0])
        supplier = int(self.sql(f"SELECT IdProveedor FROM Proveedores WHERE RUC='{ruc}'")[0])
        self.sql(f"""
            INSERT INTO Productos (SKU, NomProducto, IdCategoria, IdProveedor, Precio, StockMinimo, StockActual) VALUES
            ('UX-A-{suffix}', 'Producto A de prueba', {category}, {supplier}, 10, 1, 0),
            ('UX-B-{suffix}', 'Producto B de prueba', {category}, {supplier}, 10, 1, 0),
            ('UX-C-{suffix}', 'Producto C de prueba', {category}, {supplier}, 10, 1, 0),
            ('UX-D-{suffix}', 'Producto D de prueba', {category}, {supplier}, 10, 1, 0);
        """)
        products = {
            letter: int(self.sql(f"SELECT IdProducto FROM Productos WHERE SKU='UX-{letter}-{suffix}'")[0])
            for letter in "ABCD"
        }
        self.token = self.require("POST", "/api/auth/login", {"correo": admin_email, "contrasena": TEST_PASSWORD})["token"]
        operator_token = self.require("POST", "/api/auth/login", {"correo": operator_email, "contrasena": TEST_PASSWORD})["token"]
        product_from_api = self.require("GET", f"/api/productos/{products['A']}")
        check(product_from_api["sku"] == f"UX-A-{suffix}",
              "El backend no está conectado a la misma base MySQL aislada.")
        order_a = self.create_order(supplier, products["A"])
        order_b = self.create_order(supplier, products["B"])
        return products, order_a, order_b, operator_token

    def create_order(self, supplier, product):
        order = self.require("POST", "/api/ordenes-compra", {
            "idProveedor": supplier,
            "fechaEstimadaEntrega": None,
            "detalles": [{"idProducto": product, "cantidad": 10, "precioUnitario": 10}],
        }, 201)
        return order["idOrden"]

    def movement(self, product, quantity):
        return [{"idProducto": product, "cantidad": quantity}]

    def entry(self, order, product, quantity):
        return {"idOrden": order, "numeroGuiaRemision": f"TEST-{self.suffix}",
                "observaciones": None, "movimientos": self.movement(product, quantity)}

    def exit(self, product, quantity):
        return {"motivo": "Merma", "observaciones": None,
                "movimientos": self.movement(product, quantity)}

    def adjustment(self, product, quantity, kind):
        return {"tipoAjuste": kind, "motivo": "Conteo de prueba", "observaciones": None,
                "movimientos": self.movement(product, quantity)}

    def stock(self, product):
        return self.require("GET", f"/api/productos/{product}")["stockActual"]

    def received(self, order):
        data = self.require("GET", f"/api/ordenes-compra/{order}")
        return data["detalles"][0]["cantidadRecibida"], data["detalles"][0]["cantidadPorRecibir"], data["estado"]

    def counts(self, product):
        row = self.sql(
            "SELECT (SELECT COUNT(*) FROM Documentos), "
            "(SELECT COUNT(*) FROM MovimientosInventario), "
            f"(SELECT StockActual FROM Productos WHERE IdProducto={product})"
        )[0]
        return tuple(map(int, row.split("\t")))

    def concurrent(self, method, path, payloads):
        barrier = threading.Barrier(3)
        def send(payload):
            barrier.wait()
            return self.api(method, path, payload)
        with ThreadPoolExecutor(max_workers=2) as pool:
            futures = [pool.submit(send, payload) for payload in payloads]
            barrier.wait()
            return [future.result(timeout=40) for future in futures]

    def waits_for_product_lock(self, product):
        env = os.environ.copy()
        if self.args.mysql_password:
            env["MYSQL_PWD"] = self.args.mysql_password
        statement = (
            "START TRANSACTION; "
            f"SELECT 'LOCKED' FROM Productos WHERE IdProducto={product} FOR UPDATE; "
            "SELECT SLEEP(2); COMMIT;"
        )
        locker = subprocess.Popen(
            self.mysql_args + ["--unbuffered", "--execute", statement],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
            encoding="utf-8", env=env,
        )
        try:
            check(locker.stdout.readline().strip() == "LOCKED", "No se obtuvo el bloqueo de fila de prueba.")
            started = threading.Event()
            def request():
                started.set()
                return self.api("POST", "/api/salidas", self.exit(product, 1))
            with ThreadPoolExecutor(max_workers=1) as pool:
                future = pool.submit(request)
                check(started.wait(2), "La solicitud concurrente no comenzó.")
                time.sleep(0.3)
                check(not future.done(), "La solicitud no esperó al bloqueo del producto.")
                status, body = future.result(timeout=35)
                check(status == 201, f"La salida válida tras esperar el bloqueo falló: {status}: {body}")
            check(locker.wait(timeout=5) == 0, "La transacción de bloqueo falló.")
        finally:
            if locker.poll() is None:
                locker.terminate()
                locker.wait(timeout=5)

    def run(self):
        self.verify_database()
        products, order_a, order_b, operator_token = self.fixture()
        a, b, c, d = (products[key] for key in "ABCD")
        check(self.api("POST", "/api/ajustes", self.adjustment(d, 1, "Incremento"), operator_token)[0] == 403,
              "El Operador no debe registrar ajustes.")

        first = self.require("POST", "/api/entradas", self.entry(order_a, a, 4), 201)
        self.require("POST", "/api/entradas", self.entry(order_a, a, 3), 201)
        check(self.received(order_a) == (7, 3, "Parcial") and self.stock(a) == 7,
              "La recepción parcial no actualizó orden y stock.")
        before = self.counts(a)
        check(self.api("POST", "/api/entradas", self.entry(order_a, a, 4))[0] == 422,
              "Una entrada que supera el saldo debe rechazarse.")
        check(self.counts(a) == before, "La entrada rechazada dejó cambios parciales.")
        self.require("POST", "/api/salidas", self.exit(a, 7), 201)
        before = self.counts(a)
        check(self.api("DELETE", f"/api/entradas/{first['idEntrada']}", {"motivo": "Prueba de reversión"})[0] == 422,
              "Anular una entrada sin stock debe rechazarse.")
        check(self.counts(a) == before and self.received(order_a) == (7, 3, "Parcial"),
              "La anulación fallida modificó stock, documentos o saldo.")
        print("OK: recepciones parciales, límites y reversión fallida atómica")

        identical = self.entry(order_b, b, 2)
        pair = self.concurrent("POST", "/api/entradas", [identical, identical])
        check(sorted(status for status, _ in pair) == [201, 201],
              f"Dos recepciones concurrentes válidas deben completarse: {pair}")
        check(self.received(order_b) == (4, 6, "Parcial"), "El saldo tras doble envío no coincide.")
        competing = self.entry(order_b, b, 4)
        pair = self.concurrent("POST", "/api/entradas", [competing, competing])
        check(sorted(status for status, _ in pair) == [201, 422],
              f"Solo una recepción de 4 debe caber en el saldo restante: {pair}")
        check(self.received(order_b) == (8, 2, "Parcial") and self.stock(b) == 8,
              "La competencia por la orden dejó un saldo incorrecto.")
        original_entry = next(data for status, data in pair if status == 201)
        replacement = self.entry(order_b, b, 3) | {"motivoCorreccion": "Ajustar recepción de prueba"}
        updated_entry = self.require("PUT", f"/api/entradas/{original_entry['idEntrada']}", replacement)
        check(self.require("GET", f"/api/entradas/{original_entry['idEntrada']}")["estadoDocumento"] == 2,
              "El documento original de entrada debe quedar Rectificado.")
        check(updated_entry["idDocumentoOrigen"] == original_entry["idDocumento"] and self.received(order_b)[0] == 7,
              "La rectificación de entrada no conservó el origen o el saldo.")
        self.require("DELETE", f"/api/entradas/{updated_entry['idEntrada']}", {"motivo": "Anular reemplazo de prueba"}, 204)
        check(self.received(order_b)[0] == 4 and self.stock(b) == 4,
              "La anulación de entrada no revirtió el reemplazo.")
        print("OK: doble envío, concurrencia sobre orden, rectificación y anulación de entrada")

        self.require("POST", "/api/ajustes", self.adjustment(c, 10, "Incremento"), 201)
        before = self.counts(c)
        check(self.api("POST", "/api/salidas", self.exit(c, 11))[0] == 422,
              "La salida sin stock debe rechazarse.")
        check(self.counts(c) == before, "La salida rechazada dejó cambios parciales.")
        pair = self.concurrent("POST", "/api/salidas", [self.exit(c, 2)] * 2)
        check(sorted(status for status, _ in pair) == [201, 201] and self.stock(c) == 6,
              f"Dos salidas concurrentes válidas deben completarse: {pair}")
        pair = self.concurrent("POST", "/api/salidas", [self.exit(c, 4)] * 2)
        check(sorted(status for status, _ in pair) == [201, 422] and self.stock(c) == 2,
              f"Solo una salida de 4 debe caber: {pair}")
        original_exit = next(data for status, data in pair if status == 201)
        updated_exit = self.require("PUT", f"/api/salidas/{original_exit['idSalida']}",
                                    self.exit(c, 3) | {"motivoCorreccion": "Ajustar salida de prueba"})
        check(updated_exit["idDocumentoOrigen"] == original_exit["idDocumento"] and self.stock(c) == 3,
              "La rectificación de salida no conservó el origen o el stock.")
        self.require("DELETE", f"/api/salidas/{updated_exit['idSalida']}", {"motivo": "Anular salida de prueba"}, 204)
        check(self.stock(c) == 6, "La anulación de salida no devolvió unidades.")
        self.waits_for_product_lock(c)
        check(self.stock(c) == 5, "La solicitud en espera no actualizó el stock tras liberar el bloqueo.")
        print("OK: stock concurrente, salida rechazada atómica, rectificación y anulación de salida")

        original_adjustment = self.require("POST", "/api/ajustes", self.adjustment(d, 5, "Incremento"), 201)
        decreasing = self.require("POST", "/api/ajustes", self.adjustment(d, 3, "Decremento"), 201)
        check(self.stock(d) == 2, "Incremento y Decremento no dejaron el stock esperado.")
        before = self.counts(d)
        check(self.api("PUT", f"/api/ajustes/{original_adjustment['idAjuste']}",
                       self.adjustment(d, 4, "Incremento") | {"motivoCorreccion": "Prueba sin stock"})[0] == 422,
              "Rectificar un incremento sin stock para revertirlo debe fallar.")
        check(self.counts(d) == before, "La rectificación de ajuste fallida dejó cambios parciales.")
        self.require("DELETE", f"/api/ajustes/{decreasing['idAjuste']}", {"motivo": "Restaurar stock de prueba"}, 204)
        updated_adjustment = self.require("PUT", f"/api/ajustes/{original_adjustment['idAjuste']}",
                                          self.adjustment(d, 4, "Incremento") | {"motivoCorreccion": "Ajustar conteo de prueba"})
        check(updated_adjustment["idDocumentoOrigen"] == original_adjustment["idDocumento"] and self.stock(d) == 4,
              "La rectificación de ajuste no conservó el origen o el stock.")
        self.require("DELETE", f"/api/ajustes/{updated_adjustment['idAjuste']}", {"motivo": "Anular ajuste de prueba"}, 204)
        check(self.stock(d) == 0, "La anulación de ajuste no revirtió el stock.")
        print("OK: ajustes, reversión fallida atómica, rectificación y anulación")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api-url", default="http://127.0.0.1:8081")
    parser.add_argument("--mysql-bin", required=True)
    parser.add_argument("--mysql-port", type=int, required=True)
    parser.add_argument("--mysql-user", default="root")
    parser.add_argument("--mysql-password", default="")
    parser.add_argument("--expected-datadir", required=True)
    args = parser.parse_args()
    IsolatedRun(args).run()


if __name__ == "__main__":
    main()
