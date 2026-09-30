import { Component, input } from '@angular/core';
const paths: Record<string, string> = {
  users:
    'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M16 3a4 4 0 0 1 0 8 M22 21v-2a4 4 0 0 0-3-3.87 M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
  shield: 'M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11 M9 12l2 2 4-4',
  home: 'm3 10 9-7 9 7 M5 9v12h14V9 M9 21v-8h6v8',
  orders:
    'M8 4h11a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h1 M9 2h6v4H9z M8 11h8 M8 16h8',
  logout: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9',
  search: 'M21 21l-4.5-4.5 M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
  plus: 'M12 5v14 M5 12h14',
  arrow: 'M19 12H5 M12 19l-7-7 7-7',
  next: 'm9 5 7 7-7 7',
  edit: 'm16 3 5 5 M4 16l-1 5 5-1L21 7a2 2 0 0 0-4-4Z',
  check: 'm5 12 4 4L19 6',
  close: 'm6 6 12 12 M6 18 18 6',
  lock: 'M5 11h14v10H5z M8 11V7a4 4 0 0 1 8 0v4 M12 15v2',
  eye: 'M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12 M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
  eyeOff:
    'm3 3 18 18 M10.6 10.6a2 2 0 0 0 2.8 2.8 M9.5 5.3A12 12 0 0 1 12 5c6.5 0 10 7 10 7a20 20 0 0 1-3 4 M6 6a20 20 0 0 0-4 6s3.5 7 10 7a12 12 0 0 0 5-1',
  info: 'M12 11v6 M12 7v.01 M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0',
  ban: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0 M5 5l14 14',
  menu: 'M4 6h16 M4 12h16 M4 18h16',
  mail: 'M3 5h18v14H3z m0 0 9 7 9-7',
};
@Component({
  selector: 'iw-icon',
  template:
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path [attr.d]="paths[name()] || paths[\'info\']" /></svg>',
  styles:
    ':host{display:inline-flex;width:20px;height:20px;flex-shrink:0}svg{width:100%;height:100%}',
})
export class Icon {
  readonly name = input('info');
  readonly paths = paths;
}
