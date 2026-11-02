type PanelLanguage = 'en' | 'pl'

const dictionaries: Record<PanelLanguage, Record<string, string>> = {
  en: {
    operations: 'Operations',
    emergencies: 'Emergencies',
    templates: 'Templates',
    account: 'Account',
    events: 'Events',
    search: 'Search reports',
    all: 'All',
    open: 'Open',
    confirmed: 'Confirmed',
    rejected: 'Rejected',
    live: 'Live',
    noReports: 'No reports found',
    selectReport: 'Select an event to see details',
    theme: 'Theme',
    themeDark: 'Dark',
    language: 'Language',
    session: 'Session',
    signOut: 'Sign out',
    close: 'Close',
    profile: 'Profile',
    about: 'About',
    active: 'Active',
  },
  pl: {
    operations: 'Operacje',
    emergencies: 'Zgłoszenia',
    templates: 'Szablony',
    account: 'Konto',
    events: 'Zgłoszenia',
    search: 'Szukaj zgłoszeń',
    all: 'Wszystkie',
    open: 'Otwarte',
    confirmed: 'Potwierdzone',
    rejected: 'Odrzucone',
    live: 'Na żywo',
    noReports: 'Brak zgłoszeń',
    selectReport: 'Wybierz zgłoszenie, aby zobaczyć szczegóły',
    theme: 'Motyw',
    themeDark: 'Ciemny',
    language: 'Język',
    session: 'Sesja',
    signOut: 'Wyloguj',
    close: 'Zamknij',
    profile: 'Profil',
    about: 'O aplikacji',
    active: 'Aktywna',
  },
}

export function usePanelLocale() {
  const language = useState<PanelLanguage>('panel-language', () => 'en')

  function t(key: string): string {
    return dictionaries[language.value][key] ?? dictionaries.en[key] ?? key
  }

  function setLanguage(next: PanelLanguage) {
    language.value = next
    if (import.meta.client) {
      localStorage.setItem('sensa-panel-language', next)
    }
  }

  function restore() {
    if (!import.meta.client) return
    const stored = localStorage.getItem('sensa-panel-language')
    if (stored === 'en' || stored === 'pl') {
      language.value = stored
    }
  }

  return { language, t, setLanguage, restore }
}
