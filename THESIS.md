Celem pracy dyplomowej jest zaprojektowanie oraz implementacja prototypu systemu szybkich powiadomień kryzysowych, inspirowanego rozwiązaniem Alert RCB, opartego na architekturze mikroserwisowej, zapewniającej szybkie generowanie, przetwarzanie i dystrybucję komunikatów. System umożliwi wysyłanie alertów do użytkowników znajdujących się w określonej strefie zagrożenia oraz integrację różnych służb ratunkowych w jednym panelu operacyjnym. W ramach projektu powstaną trzy interfejsy: aplikacje mobilne na Android i iOS oraz serwis WWW dla służb. Dodatkowo system umożliwi zgłaszanie zdarzeń przez użytkowników wraz z załącznikami, które będą przechowywane w Amazon S3. W celu zapewnienia bezpieczeństwa przewidziano moduł automatycznej moderacji treści, wykorzystujący modele uczenia maszynowego do analizy i poprawy jakości obrazów.

Architektura i komunikacja:
- Java i Kotlin + Spring Boot jako główny stos do tworzenia mikroserwisów; Kotlin (Android), Swift (iOS), Rust dla komponentów wysokowydajnych
- PostgreSQL jako główna relacyjna baza danych
- Redis jako cache i warstwa przyspieszająca
- MongoDB do przechowywania logów, dokumentów oraz powiadomień i ich szablonów.
- Amazon S3 do przechowywania plików użytkowników (załączniki)
- Kafka oraz RabbitMQ – wykorzystywane zależnie od typu komunikacji:
  Kafka do komunikacji między mikroserwisami i przetwarzania zdarzeń
  RabbitMQ do obsługi powiadomień (push, e-mail) i kolejkowania zadań
- Docker do konteneryzacji i uruchamiania komponentów
  System umożliwi wysyłkę powiadomień push i e-mail oraz odbiór alertów w czasie zbliżonym do rzeczywistego.

Zakres realizacji pracy dyplomowej:
- analiza problemu, przegląd istniejących rozwiązań typu Alert RCB / systemów kryzysowych oraz określenie wymagań funkcjonalnych i niefunkcjonalnych
- zaprojektowanie architektury mikroserwisowej systemu (podział na usługi, komunikację, przepływ danych)
- implementacja prototypu backendu w oparciu o Spring Boot (Java/Kotlin) oraz komponentów wydajnościowych w Rust
- implementacja komunikacji asynchronicznej z wykorzystaniem Kafka i RabbitMQ (dobór zastosowań dla każdego brokera)
- implementacja modułu przechowywania załączników w Amazon S3 (upload, linkowanie do zgłoszeń, bezpieczeństwo dostępu)
- opracowanie logiki rejestracji użytkownika z dodatkowymi danymi adresowymi jako alternatywa dla geolokalizacji
- opracowanie aplikacji webowej (panel) w Vue + TypeScript oraz założeń integracji z aplikacjami mobilnymi
- zaprojektowanie osobnego serwisu WWW dla służb ratunkowych: utworzenie alertów, szablonów, szybkiej publikacji komunikatów
- implementacja/integracja modułu ML do analizy zdjęć pod kątem treści nieprzyzwoitych (moderacja)
- testy funkcjonalne i wydajnościowe prototypu oraz analiza wyników
- opracowanie wniosków końcowych oraz propozycji dalszego rozwoju systemu

Wymagane umiejętności:
- znajomość technologii i języków backendowych: Java i Kotlin (Spring Boot), Rust, podstawy architektury mikroserwisowej
- znajomość baz danych (PostgreSQL, MongoDB) oraz cache (Redis)
- znajomość komunikacji asynchronicznej i brokerów wiadomości (Kafka, RabbitMQ)
- podstawowa znajomość chmury i storage obiektowego (Amazon S3)
- znajomość technologii i języków frontendowych: Vue + TypeScript
- znajomość technologii programowania aplikacji mobilnych: tworzenie natywnych aplikacji na Android (Kotlin) oraz iOS (Swift).
- znajomość podstaw ML/integracji gotowych rozwiązań moderacji obrazów (model klasyfikacyjny lub usługa)

Planowane do zastosowania języki programowania i technologie informatyczne:
Java, Kotlin, Rust, Swift, Spring Boot, PostgreSQL, Redis, MongoDB, Kafka, RabbitMQ, Docker, Amazon S3, Vue, TypeScript, push notifications

Literatura podstawowa:
1. Kleppmann M., Designing Data-Intensive Applications, O'Reilly
2. Newman S., Building Microservices: Designing Fine-Grained Systems, O'Reilly
3. https://kafka.apache.org/documentation/
4. https://aws.amazon.com/s3/