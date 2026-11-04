type PanelLanguage = 'en' | 'pl'

const dictionaries: Record<PanelLanguage, Record<string, string>> = {
  en: {
    operations: 'Operations',
    emergencies: 'Emergencies',
    templates: 'Templates',
    notify: 'Notify users',
    testLab: 'Test Lab',
    account: 'Account',
    events: 'Events',
    sentByMe: 'Sent by me',
    userReports: 'User reports',
    search: 'Search reports',
    searchAlerts: 'Search alerts',
    noAlerts: 'No alerts sent yet',
    selectAlert: 'Select an alert to see details',
    sendAlert: 'Send alert',
    delivered: 'Delivered',
    filtered: 'Filtered',
    allUsers: 'All users',
    byLocation: 'By location',
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
    notify: 'Powiadom użytkowników',
    testLab: 'Test Lab',
    account: 'Konto',
    events: 'Zgłoszenia',
    sentByMe: 'Wysłane przeze mnie',
    userReports: 'Zgłoszenia użytkowników',
    search: 'Szukaj zgłoszeń',
    searchAlerts: 'Szukaj alertów',
    noAlerts: 'Brak wysłanych alertów',
    selectAlert: 'Wybierz alert, aby zobaczyć szczegóły',
    sendAlert: 'Wyślij alert',
    delivered: 'Dostarczono',
    filtered: 'Odfiltrowano',
    allUsers: 'Wszyscy użytkownicy',
    byLocation: 'Według lokalizacji',
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
