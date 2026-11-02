import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { PageHeaderComponent } from '../../layout/page-header/page-header.component';
import { PublicCopy, PublicSiteAdmin } from '../public/public.model';
import { PublicService } from '../public/public.service';

@Component({
  selector: 'app-website-editor',
  imports: [TranslateModule, ReactiveFormsModule, MatButtonModule, PageHeaderComponent],
  template: `
    <div class="page">
      <app-page-header [title]="'website.title' | translate" [subtitle]="'website.subtitle' | translate" />
      @if (error) {
        <p class="error">{{ error | translate }}</p>
      }
      @if (message) {
        <p class="banner success">{{ message | translate }}</p>
      }
      <form class="stack" [formGroup]="form" (ngSubmit)="save()">
        <section class="card">
          <label class="check">
            <input type="checkbox" formControlName="published" />
            {{ 'website.published' | translate }}
          </label>
          <label>
            {{ 'website.principalName' | translate }}
            <input formControlName="principalName" />
          </label>
          <label>
            {{ 'website.heroImageUrl' | translate }}
            <input formControlName="heroImageUrl" />
          </label>
        </section>
        <section class="card" formGroupName="en">
          <h2>{{ 'website.copyEn' | translate }}</h2>
          <label>{{ 'website.tagline' | translate }}<input formControlName="tagline" /></label>
          <label>{{ 'website.about' | translate }}<textarea formControlName="about" rows="4"></textarea></label>
          <label>{{ 'website.principalMessage' | translate }}<textarea formControlName="principalMessage" rows="4"></textarea></label>
          <label>{{ 'website.facilities' | translate }}<textarea formControlName="facilities" rows="4"></textarea></label>
        </section>
        <section class="card" formGroupName="hi">
          <h2>{{ 'website.copyHi' | translate }}</h2>
          <label>{{ 'website.tagline' | translate }}<input formControlName="tagline" /></label>
          <label>{{ 'website.about' | translate }}<textarea formControlName="about" rows="4"></textarea></label>
          <label>{{ 'website.principalMessage' | translate }}<textarea formControlName="principalMessage" rows="4"></textarea></label>
          <label>{{ 'website.facilities' | translate }}<textarea formControlName="facilities" rows="4"></textarea></label>
        </section>
        <section class="card" formGroupName="ur">
          <h2>{{ 'website.copyUr' | translate }}</h2>
          <label>{{ 'website.tagline' | translate }}<input formControlName="tagline" /></label>
          <label>{{ 'website.about' | translate }}<textarea formControlName="about" rows="4"></textarea></label>
          <label>{{ 'website.principalMessage' | translate }}<textarea formControlName="principalMessage" rows="4"></textarea></label>
          <label>{{ 'website.facilities' | translate }}<textarea formControlName="facilities" rows="4"></textarea></label>
        </section>
        <button mat-flat-button color="primary" type="submit" [disabled]="saving">
          {{ 'common.save' | translate }}
        </button>
      </form>
    </div>
  `,
  styles: `
    .stack { display: flex; flex-direction: column; gap: 16px; }
    .card { padding: 16px; border: 1px solid var(--color-border); border-radius: 12px; display: grid; gap: 12px; }
    label { display: grid; gap: 6px; font-size: .9rem; }
    .check { display: flex; gap: 8px; align-items: center; }
    input, textarea { padding: 8px 10px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; }
    .error { color: #b91c1c; }
    .banner.success { color: #166534; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WebsiteEditorComponent implements OnInit {
  error = '';
  message = '';
  saving = false;
  form;

  constructor(private publicApi: PublicService, private fb: FormBuilder, private cdr: ChangeDetectorRef) {
    this.form = this.fb.nonNullable.group({
      published: [true],
      principalName: [''],
      heroImageUrl: [''],
      en: this.copyGroup(),
      hi: this.copyGroup(),
      ur: this.copyGroup(),
    });
  }

  ngOnInit(): void {
    this.publicApi.adminSite().subscribe({
      next: (site) => {
        this.patch(site);
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'website.loadFailed';
        this.cdr.markForCheck();
      },
    });
  }

  save(): void {
    this.saving = true;
    this.error = '';
    this.message = '';
    const value = this.form.getRawValue();
    this.publicApi
      .saveSite({
        published: value.published,
        principalName: value.principalName || null,
        heroImageUrl: value.heroImageUrl || null,
        copies: [
          this.toCopy('en', value.en),
          this.toCopy('hi', value.hi),
          this.toCopy('ur', value.ur),
        ],
      })
      .subscribe({
        next: (site) => {
          this.saving = false;
          this.message = 'website.saved';
          this.patch(site);
          this.cdr.markForCheck();
        },
        error: () => {
          this.saving = false;
          this.error = 'website.saveFailed';
          this.cdr.markForCheck();
        },
      });
  }

  private copyGroup() {
    return this.fb.nonNullable.group({
      tagline: [''],
      about: [''],
      principalMessage: [''],
      facilities: [''],
    });
  }

  private patch(site: PublicSiteAdmin): void {
    this.form.patchValue({
      published: site.published,
      principalName: site.principalName ?? '',
      heroImageUrl: site.heroImageUrl ?? '',
      en: this.fromCopy(site.copies, 'en'),
      hi: this.fromCopy(site.copies, 'hi'),
      ur: this.fromCopy(site.copies, 'ur'),
    });
  }

  private fromCopy(copies: PublicCopy[], locale: string) {
    const copy = copies.find((item) => item.locale === locale);
    return {
      tagline: copy?.tagline ?? '',
      about: copy?.about ?? '',
      principalMessage: copy?.principalMessage ?? '',
      facilities: copy?.facilities ?? '',
    };
  }

  private toCopy(locale: string, value: { tagline: string; about: string; principalMessage: string; facilities: string }): PublicCopy {
    return {
      locale,
      tagline: value.tagline || null,
      about: value.about || null,
      principalMessage: value.principalMessage || null,
      facilities: value.facilities || null,
    };
  }
}
