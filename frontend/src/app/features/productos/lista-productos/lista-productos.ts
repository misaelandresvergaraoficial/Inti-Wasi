import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoService } from '../services/producto';
import { Producto } from '../models/producto.model';

@Component({
  selector: 'app-lista-productos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lista-productos.html'
})
export class ListaProductosComponent implements OnInit {
  private productoService = inject(ProductoService);
  private cdr = inject(ChangeDetectorRef);

  productos: Producto[] = [];
  productosFiltrados: Producto[] = [];
  terminoBusqueda = '';
  loading = true;
  errorMessage = '';

  // Variables del Modal
  mostrarModal = false;
  guardando = false;
  modoEdicion = false; // <-- Bandera para saber si es Editar
  idProductoActual: number | null = null; // <-- Guarda el ID a editar
  
  nuevoProducto: any = {
    sku: '', nomProducto: '', idCategoria: null, idProveedor: null, precio: null, stockMinimo: null
  };

  ngOnInit(): void {
    this.cargarProductos();
  }

  cargarProductos(): void {
    this.productoService.getProductos().subscribe({
      next: (data) => {
        this.productos = data;
        this.productosFiltrados = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error("Error al cargar:", err);
        this.errorMessage = 'No se pudo cargar el catálogo.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  filtrarProductos(): void {
    const termino = this.terminoBusqueda.toLowerCase();
    this.productosFiltrados = this.productos.filter(prod => 
      prod.nomProducto.toLowerCase().includes(termino) ||
      prod.sku.toLowerCase().includes(termino) ||
      prod.nomCategoria.toLowerCase().includes(termino)
    );
  }

  // --- LÓGICA DEL MODAL (CREAR / EDITAR) ---

  abrirModalNuevo(): void {
    this.modoEdicion = false;
    this.idProductoActual = null;
    this.nuevoProducto = { sku: '', nomProducto: '', idCategoria: null, idProveedor: null, precio: null, stockMinimo: null };
    this.mostrarModal = true;
    this.cdr.detectChanges();
  }

  abrirModalEditar(prod: Producto): void {
    this.modoEdicion = true;
    this.idProductoActual = prod.idProducto;
    // Llenamos el formulario con los datos actuales
    this.nuevoProducto = { 
      sku: prod.sku, 
      nomProducto: prod.nomProducto, 
      idCategoria: prod.idCategoria, 
      idProveedor: prod.idProveedor, 
      precio: prod.precio, 
      stockMinimo: prod.stockMinimo 
    };
    this.mostrarModal = true;
    this.cdr.detectChanges();
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.cdr.detectChanges();
  }

  guardarProducto(): void {
    if (!this.nuevoProducto.sku || !this.nuevoProducto.nomProducto || !this.nuevoProducto.precio) {
      alert('Complete los campos obligatorios.');
      return;
    }

    this.guardando = true;
    this.cdr.detectChanges();

    if (this.modoEdicion && this.idProductoActual) {
      // Petición PUT
      this.productoService.actualizarProducto(this.idProductoActual, this.nuevoProducto).subscribe({
        next: () => this.finalizarGuardado(),
        error: (err) => this.manejarErrorGuardado(err)
      });
    } else {
      // Petición POST
      this.productoService.crearProducto(this.nuevoProducto).subscribe({
        next: () => this.finalizarGuardado(),
        error: (err) => this.manejarErrorGuardado(err)
      });
    }
  }

  private finalizarGuardado(): void {
    this.guardando = false;
    this.cerrarModal();
    this.cargarProductos();
  }

  private manejarErrorGuardado(err: any): void {
    console.error('Error:', err);
    alert('Ocurrió un error en la operación.');
    this.guardando = false;
    this.cdr.detectChanges();
  }

  // --- LÓGICA DE ELIMINAR / DESACTIVAR ---

  eliminarProducto(id: number): void {
    if (confirm('¿Está seguro de cambiar el estado de este producto?')) {
      this.productoService.eliminarProducto(id).subscribe({
        next: () => this.cargarProductos(),
        error: (err) => {
          console.error(err);
          alert('Error al actualizar el estado del producto.');
        }
      });
    }
  }
}