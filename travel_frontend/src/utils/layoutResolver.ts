export type AppLayout = 'topnav' | 'landing' | 'router-view'

const LAYOUT_MAP = {
  landing: 'landing',
  none: 'router-view',
} as const

function isMappedLayout(layout: string): layout is keyof typeof LAYOUT_MAP {
  return layout in LAYOUT_MAP
}

export function resolveLayout(layout: unknown): AppLayout {
  if (typeof layout !== 'string') {
    return 'topnav'
  }

  const normalizedLayout = layout.trim().toLowerCase()

  if (!normalizedLayout) {
    return 'topnav'
  }

  if (isMappedLayout(normalizedLayout)) {
    return LAYOUT_MAP[normalizedLayout]
  }

  return 'topnav'
}
