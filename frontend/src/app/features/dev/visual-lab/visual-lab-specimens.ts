/** One competing visual variant with provenance for the Visual Lab. */
export interface VisualSpecimen {
  id: string;
  label: string;
  /** CSS class(es) applied to the live sample host (under .vl-scope). */
  sampleClass: string;
  provenance: {
    file: string;
    selector: string;
    surface: string;
  };
}

export interface VisualGroup {
  id: string;
  title: string;
  description: string;
  specimens: VisualSpecimen[];
}

export const VISUAL_LAB_GROUPS: VisualGroup[] = [
  {
    id: 'brand',
    title: 'Brand / wordmark',
    description: 'Product name treatments across SPA shell and Keycloak login.',
    specimens: [
      {
        id: 'brand-navbar',
        label: 'Navbar SUCCESSHUB',
        sampleClass: 'vl-brand-navbar',
        provenance: {
          file: 'frontend/src/app/shared/components/navbar/navbar.component.scss',
          selector: '.brand',
          surface: 'Navbar',
        },
      },
      {
        id: 'brand-register',
        label: 'Keycloak register SuccessHub',
        sampleClass: 'vl-brand-register',
        provenance: {
          file: 'docker/themes/successhub/login/resources/css/successhub.css',
          selector: '.sh-enroll__brand / .sh-enroll__title-*',
          surface: 'Keycloak register',
        },
      },
      {
        id: 'brand-login-portal',
        label: 'Keycloak Sovereign Portal',
        sampleClass: 'vl-brand-login-portal',
        provenance: {
          file: 'docker/themes/successhub/login/resources/css/successhub.css',
          selector: '#kc-page-title / login header',
          surface: 'Keycloak login',
        },
      },
    ],
  },
  {
    id: 'primary',
    title: 'Primary CTA',
    description: 'Main action buttons that should share one look.',
    specimens: [
      {
        id: 'primary-gold-btn',
        label: 'Global .gold-btn',
        sampleClass: 'gold-btn',
        provenance: {
          file: 'frontend/src/global.css',
          selector: '.gold-btn',
          surface: 'Sidebar / Placeholder',
        },
      },
      {
        id: 'primary-tasks',
        label: 'Tasks .btn-primary',
        sampleClass: 'vl-primary-tasks',
        provenance: {
          file: 'frontend/src/app/features/tasks/tasks.component.scss',
          selector: '.btn-primary',
          surface: 'Quest Log',
        },
      },
      {
        id: 'primary-goals',
        label: 'Goals .btn-primary',
        sampleClass: 'vl-primary-goals',
        provenance: {
          file: 'frontend/src/app/features/goals/goals.component.scss',
          selector: '.btn-primary',
          surface: 'Boss Battle',
        },
      },
      {
        id: 'primary-settings-flat',
        label: 'Settings flat .btn-primary',
        sampleClass: 'vl-primary-settings',
        provenance: {
          file: 'frontend/src/app/features/settings/settings.component.scss',
          selector: '.btn-primary',
          surface: 'Settings',
        },
      },
      {
        id: 'primary-glitch',
        label: 'app-glitch-button',
        sampleClass: 'vl-primary-glitch',
        provenance: {
          file: 'frontend/src/app/shared/components/glitch-button/',
          selector: 'app-glitch-button / .glitch-btn',
          surface: 'Settings',
        },
      },
      {
        id: 'primary-loot-hero',
        label: 'Loot .hero__cta',
        sampleClass: 'vl-primary-loot',
        provenance: {
          file: 'frontend/src/app/features/loot-boxes/loot-boxes.component.scss',
          selector: '.hero__cta',
          surface: 'Celestial Cache',
        },
      },
      {
        id: 'primary-home-celebrate',
        label: 'Home .celebrate-btn',
        sampleClass: 'vl-primary-celebrate',
        provenance: {
          file: 'frontend/src/app/features/home/home.component.scss',
          selector: '.celebrate-btn',
          surface: 'Dashboard',
        },
      },
      {
        id: 'primary-kc-submit',
        label: 'Keycloak .sh-submit',
        sampleClass: 'vl-primary-kc-submit',
        provenance: {
          file: 'docker/themes/successhub/login/resources/css/successhub.css',
          selector: '.btn-primary / .sh-submit',
          surface: 'Keycloak login',
        },
      },
      {
        id: 'primary-kc-enroll',
        label: 'Keycloak .sh-enroll__submit',
        sampleClass: 'vl-primary-kc-enroll',
        provenance: {
          file: 'docker/themes/successhub/login/resources/css/successhub.css',
          selector: '.sh-enroll__submit',
          surface: 'Keycloak register',
        },
      },
    ],
  },
  {
    id: 'secondary',
    title: 'Secondary / ghost',
    description: 'Cancel, outline, and quieter actions.',
    specimens: [
      {
        id: 'secondary-ghost',
        label: 'Notch outline (global .ghost-btn)',
        sampleClass: 'ghost-btn',
        provenance: {
          file: 'frontend/src/global.css',
          selector: '.ghost-btn · 45° cut; gold rim+label on hover',
          surface: 'W15 winner · app-wide',
        },
      },
      {
        id: 'secondary-tasks',
        label: 'Tasks .btn-secondary',
        sampleClass: 'vl-secondary-tasks',
        provenance: {
          file: 'frontend/src/app/features/tasks/tasks.component.scss',
          selector: '.btn-secondary',
          surface: 'Quest Log',
        },
      },
      {
        id: 'secondary-summon',
        label: 'Goals .summon-btn',
        sampleClass: 'vl-secondary-summon',
        provenance: {
          file: 'frontend/src/app/features/goals/goals.component.scss',
          selector: '.summon-btn',
          surface: 'Boss Battle',
        },
      },
      {
        id: 'secondary-loot',
        label: 'Loot .hero__cta-secondary',
        sampleClass: 'vl-secondary-loot',
        provenance: {
          file: 'frontend/src/app/features/loot-boxes/loot-boxes.component.scss',
          selector: '.hero__cta-secondary',
          surface: 'Celestial Cache',
        },
      },
      {
        id: 'secondary-kc-default',
        label: 'Keycloak .btn-default',
        sampleClass: 'vl-secondary-kc',
        provenance: {
          file: 'docker/themes/successhub/login/resources/css/successhub.css',
          selector: '.btn-default',
          surface: 'Keycloak',
        },
      },
    ],
  },
  {
    id: 'danger',
    title: 'Danger',
    description: 'Destructive actions.',
    specimens: [
      {
        id: 'danger-glitch',
        label: 'glitch-btn--danger',
        sampleClass: 'vl-danger-glitch',
        provenance: {
          file: 'frontend/src/app/shared/components/glitch-button/glitch-button.component.scss',
          selector: '.glitch-btn--danger',
          surface: 'Settings',
        },
      },
      {
        id: 'danger-settings',
        label: 'Settings .btn-danger-outline',
        sampleClass: 'vl-danger-settings',
        provenance: {
          file: 'frontend/src/app/features/settings/settings.component.scss',
          selector: '.btn-danger-outline',
          surface: 'Settings danger zone',
        },
      },
      {
        id: 'danger-streak',
        label: 'Streak .streak-btn--danger',
        sampleClass: 'vl-danger-streak',
        provenance: {
          file: 'frontend/src/app/features/profile/streak-panel/streak-panel.component.scss',
          selector: '.streak-btn--danger',
          surface: 'Profile streak',
        },
      },
      {
        id: 'danger-dev',
        label: 'Dev .dev-btn--danger',
        sampleClass: 'vl-danger-dev',
        provenance: {
          file: 'frontend/src/app/shared/components/dev-tools/dev-tools-host.component.scss',
          selector: '.dev-btn--danger',
          surface: 'Dev bubble',
        },
      },
    ],
  },
  {
    id: 'link',
    title: 'Links / navigation CTAs',
    description: 'Go-somewhere patterns that are not full buttons.',
    specimens: [
      {
        id: 'link-nav',
        label: 'Sidebar .nav-link',
        sampleClass: 'vl-link-nav',
        provenance: {
          file: 'frontend/src/app/shared/components/sidebar/sidebar.component.scss',
          selector: '.nav-link',
          surface: 'Sidebar',
        },
      },
      {
        id: 'link-muted',
        label: 'Home .link-muted',
        sampleClass: 'vl-link-muted',
        provenance: {
          file: 'frontend/src/app/features/home/home.component.scss',
          selector: '.link-muted',
          surface: 'Dashboard',
        },
      },
      {
        id: 'link-deep',
        label: 'Home .task-deep-link',
        sampleClass: 'vl-link-deep',
        provenance: {
          file: 'frontend/src/app/features/home/home.component.scss',
          selector: '.task-deep-link',
          surface: 'Dashboard',
        },
      },
      {
        id: 'link-inventory',
        label: 'Profile .inventory__cta',
        sampleClass: 'vl-link-inventory',
        provenance: {
          file: 'frontend/src/app/features/profile/profile.component.scss',
          selector: '.inventory__cta',
          surface: 'Profile',
        },
      },
      {
        id: 'link-kc',
        label: 'Keycloak gold text link',
        sampleClass: 'vl-link-kc',
        provenance: {
          file: 'docker/themes/successhub/login/resources/css/successhub.css',
          selector: 'a / .sh-forgot',
          surface: 'Keycloak',
        },
      },
    ],
  },
  {
    id: 'surface',
    title: 'Surfaces / panels / cards',
    description: 'Containers for content blocks.',
    specimens: [
      {
        id: 'surface-card',
        label: 'Global .surface-card',
        sampleClass: 'surface-card vl-surface-pad',
        provenance: {
          file: 'frontend/src/global.css',
          selector: '.surface-card',
          surface: 'Home / Goals',
        },
      },
      {
        id: 'surface-tasks-column',
        label: 'Tasks .column',
        sampleClass: 'vl-surface-column',
        provenance: {
          file: 'frontend/src/app/features/tasks/tasks.component.scss',
          selector: '.column',
          surface: 'Quest Log',
        },
      },
      {
        id: 'surface-merit',
        label: 'Achievements .merit-card',
        sampleClass: 'vl-surface-merit',
        provenance: {
          file: 'frontend/src/app/features/achievements/achievements.component.ts',
          selector: '.merit-card',
          surface: 'Achievements',
        },
      },
      {
        id: 'surface-side',
        label: 'Settings .side-card',
        sampleClass: 'vl-surface-side',
        provenance: {
          file: 'frontend/src/app/features/settings/settings.component.scss',
          selector: '.side-card',
          surface: 'Settings',
        },
      },
      {
        id: 'surface-kc-card',
        label: 'Keycloak .card-pf',
        sampleClass: 'vl-surface-kc-card',
        provenance: {
          file: 'docker/themes/successhub/login/resources/css/successhub.css',
          selector: '.card-pf',
          surface: 'Keycloak login',
        },
      },
    ],
  },
  {
    id: 'form',
    title: 'Forms / inputs',
    description: 'Text fields and checkbox personality.',
    specimens: [
      {
        id: 'form-settings',
        label: 'Settings .field__input',
        sampleClass: 'vl-form-settings',
        provenance: {
          file: 'frontend/src/app/features/settings/settings.component.scss',
          selector: '.field__input',
          surface: 'Settings',
        },
      },
      {
        id: 'form-goals',
        label: 'Goals .form-field input',
        sampleClass: 'vl-form-goals',
        provenance: {
          file: 'frontend/src/app/features/goals/goals.component.scss',
          selector: '.form-field input',
          surface: 'Boss Battle',
        },
      },
      {
        id: 'form-tasks',
        label: 'Tasks .add-form__input',
        sampleClass: 'vl-form-tasks',
        provenance: {
          file: 'frontend/src/app/features/tasks/tasks.component.scss',
          selector: '.add-form__input',
          surface: 'Quest Log',
        },
      },
      {
        id: 'form-kc-input',
        label: 'Keycloak .sh-input',
        sampleClass: 'vl-form-kc',
        provenance: {
          file: 'docker/themes/successhub/login/resources/css/successhub.css',
          selector: '.sh-input',
          surface: 'Keycloak login',
        },
      },
      {
        id: 'form-checkbox-native',
        label: 'Native gold checkbox',
        sampleClass: 'vl-form-check-native',
        provenance: {
          file: 'frontend/src/app/features/goals/goals.component.scss',
          selector: 'input[type=checkbox]',
          surface: 'Goals / Tasks',
        },
      },
      {
        id: 'form-checkbox-glitch',
        label: 'app-glitch-checkbox',
        sampleClass: 'vl-form-check-glitch',
        provenance: {
          file: 'frontend/src/app/shared/components/glitch-checkbox/',
          selector: 'app-glitch-checkbox',
          surface: 'Settings',
        },
      },
    ],
  },
  {
    id: 'btn-pair',
    title: 'Button pairs (shape language)',
    description:
      'Primary + secondary side by side. Pick one family so Cancel/Save and Settings rows share geometry.',
    specimens: [
      {
        id: 'pair-current',
        label: 'A · Current clash',
        sampleClass: 'vl-pair',
        provenance: {
          file: 'glitch-btn + .ghost-btn',
          selector: 'glitch fill + rounded ghost',
          surface: 'Settings / Tasks (today)',
        },
      },
      {
        id: 'pair-notch-outline',
        label: 'B · Notch family (outline secondary)',
        sampleClass: 'vl-pair',
        provenance: {
          file: 'proposed',
          selector: 'same 45° cut; ghost outline, no stripe',
          surface: 'Recommended',
        },
      },
      {
        id: 'pair-notch-stripe',
        label: 'C · Notch family (gray stripe)',
        sampleClass: 'vl-pair',
        provenance: {
          file: 'proposed',
          selector: 'same cut + muted gray edge stripe',
          surface: 'Proposed',
        },
      },
      {
        id: 'pair-notch-fill',
        label: 'D · Notch family (filled ghost)',
        sampleClass: 'vl-pair',
        provenance: {
          file: 'proposed',
          selector: 'same cut; surface fill, no stripe',
          surface: 'Proposed',
        },
      },
      {
        id: 'pair-rounded',
        label: 'E · Rounded everywhere',
        sampleClass: 'vl-pair',
        provenance: {
          file: '.gold-btn + .ghost-btn',
          selector: 'shared radius; gold = color only',
          surface: 'Loses glitch identity',
        },
      },
    ],
  },
  {
    id: 'icon-btn',
    title: 'Icon buttons',
    description:
      'Edit vs Add should share one shape; differ by outline vs fill only.',
    specimens: [
      {
        id: 'icon-current',
        label: 'A · Current clash',
        sampleClass: 'vl-icon-row',
        provenance: {
          file: 'tasks.component.scss',
          selector: '.icon-btn + .hex-add',
          surface: 'Quest Log (today)',
        },
      },
      {
        id: 'icon-chamfer',
        label: 'B · Chamfer square pair',
        sampleClass: 'vl-icon-row',
        provenance: {
          file: 'proposed',
          selector: '45° cut square · outline edit / fill add',
          surface: 'Matches glitch CTA',
        },
      },
      {
        id: 'icon-hex',
        label: 'C · Hex pair',
        sampleClass: 'vl-icon-row',
        provenance: {
          file: 'proposed',
          selector: 'hex · outline edit / fill add',
          surface: 'Matches hex-add language',
        },
      },
      {
        id: 'icon-round',
        label: 'D · Rounded square pair',
        sampleClass: 'vl-icon-row',
        provenance: {
          file: 'proposed',
          selector: '8px radius · outline edit / fill add',
          surface: 'Generic web',
        },
      },
    ],
  },
];

/** Seed picks used for W15 when the operator has not chosen yet. */
export const RECOMMENDED_PICKS: Record<string, string> = {
  brand: 'brand-navbar',
  primary: 'primary-glitch',
  secondary: 'secondary-ghost',
  danger: 'danger-glitch',
  link: 'link-deep',
  surface: 'surface-card',
  form: 'form-settings',
  'btn-pair': 'pair-notch-outline',
  'icon-btn': 'icon-chamfer',
};
