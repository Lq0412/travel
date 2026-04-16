import { computed, readonly, shallowRef } from 'vue'

export interface HomeDestinationTheme {
  accent: string
  surface: string
  overlay: string
  text: string
}

export interface HomeDestination {
  id: 'tokyo' | 'paris' | 'iceland' | 'island'
  name: string
  tagline: string
  prompt: string
  location: string
  season: string
  heroImage: string
  theme: HomeDestinationTheme
}

type DeepReadonly<T> = {
  readonly [K in keyof T]: T[K] extends object ? DeepReadonly<T[K]> : T[K]
}

function freezeHomeDestination(destination: HomeDestination): DeepReadonly<HomeDestination> {
  return Object.freeze({
    ...destination,
    theme: Object.freeze({ ...destination.theme }),
  })
}

export const HOME_DESTINATIONS = Object.freeze([
  freezeHomeDestination({
    id: 'tokyo',
    name: 'Tokyo After Dark',
    tagline: 'Neon avenues, omakase counters, and skyline nights.',
    prompt: '东京 3 天轻松城市漫游，预算 5000 元，想看夜景也想吃好吃的。',
    location: 'Japan',
    season: 'Spring',
    heroImage:
      'https://images.unsplash.com/photo-1542051841857-5f90071e7989?auto=format&fit=crop&w=1600&q=80',
    theme: {
      accent: '#86a8ff',
      surface: '#111827',
      overlay: 'rgba(10, 14, 27, 0.56)',
      text: '#f8fafc',
    },
  }),
  freezeHomeDestination({
    id: 'paris',
    name: 'Paris Left Bank',
    tagline: 'Gallery mornings, cafe terraces, and golden-hour boulevards.',
    prompt: '巴黎 5 天艺术和咖啡馆路线，适合拍照，节奏从容一点。',
    location: 'France',
    season: 'Autumn',
    heroImage:
      'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1600&q=80',
    theme: {
      accent: '#f4c58f',
      surface: '#f5efe6',
      overlay: 'rgba(78, 52, 35, 0.28)',
      text: '#f8f4ef',
    },
  }),
  freezeHomeDestination({
    id: 'iceland',
    name: 'Iceland Horizon',
    tagline: 'Lava fields, black-sand coasts, and geothermal escapes.',
    prompt: '冰岛 7 天自驾环岛，想看瀑布、黑沙滩和极光。',
    location: 'Iceland',
    season: 'Winter',
    heroImage:
      'https://images.unsplash.com/photo-1476610182048-b716b8518aae?auto=format&fit=crop&w=1600&q=80',
    theme: {
      accent: '#9ed8e3',
      surface: '#dbeafe',
      overlay: 'rgba(7, 24, 36, 0.42)',
      text: '#f3fbff',
    },
  }),
  freezeHomeDestination({
    id: 'island',
    name: 'Island Retreat',
    tagline: 'Lagoon mornings, barefoot dinners, and slow tropical light.',
    prompt: '海岛 4 天度假路线，想轻松一点，也想有落日和海边餐厅。',
    location: 'Indian Ocean',
    season: 'Summer',
    heroImage:
      'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1600&q=80',
    theme: {
      accent: '#ffd8a3',
      surface: '#ecfeff',
      overlay: 'rgba(98, 60, 35, 0.28)',
      text: '#fff9f2',
    },
  }),
]) as readonly DeepReadonly<HomeDestination>[]

export const defaultHomeDestination = HOME_DESTINATIONS[0]

const destinationById = new Map(
  HOME_DESTINATIONS.map((destination) => [destination.id, destination] as const),
)

export function getHomeDestinationById(id: string) {
  return destinationById.get(id as HomeDestination['id'])
}

export function useHomeDestinations(initialDestinationId = defaultHomeDestination.id) {
  const initialDestination = getHomeDestinationById(initialDestinationId) ?? defaultHomeDestination
  const resetDestinationId = initialDestination.id
  const activeDestinationId = shallowRef<HomeDestination['id']>(initialDestination.id)
  const destinations = computed(() => HOME_DESTINATIONS)

  const activeDestination = computed(
    () => getHomeDestinationById(activeDestinationId.value) ?? defaultHomeDestination,
  )

  function setActiveDestination(id: string) {
    const destination = getHomeDestinationById(id)
    if (!destination) {
      return false
    }

    activeDestinationId.value = destination.id
    return true
  }

  function resetActiveDestination() {
    activeDestinationId.value = resetDestinationId
  }

  return {
    destinations,
    activeDestination,
    activeDestinationId: readonly(activeDestinationId),
    setActiveDestination,
    resetActiveDestination,
  }
}
