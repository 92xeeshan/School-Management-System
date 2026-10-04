import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Dir } from '@angular/cdk/bidi';
import { map } from 'rxjs';
import { LocaleService } from './i18n/locale.service';
import { LOCALE_META } from './i18n/i18n.config';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Dir, AsyncPipe],
  template: `<div class="app-shell" [dir]="(dir$ | async) ?? 'ltr'"><router-outlet /></div>`,
  styles: `:host, .app-shell { display: block; height: 100%; }`,
})
export class AppComponent {
  private readonly locale = inject(LocaleService);
  readonly dir$ = this.locale.locale$.pipe(map((locale) => LOCALE_META[locale].dir));
}
