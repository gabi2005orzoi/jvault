~~### Faza 1: Fundația și Modelarea Datelor (Zilele 1-2)
Înainte să scrii vreo linie de logică, trebuie să ai structura de date solidă.

1.  **Inițializarea:**
    * Folosește [start.spring.io](https://start.spring.io/).
    * Dependințe: Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok, Validation.
2.  **Baza de date (Docker):**
    * Instalează Docker Desktop și pornește un container de PostgreSQL. E mult mai profesionist decât să instalezi Postgres direct pe Windows/Mac.
3.  **Entitățile (Modelul):**
    * Aici este capcana numărul 1. **NU folosi `double` sau `float` pentru bani!** Aceste tipuri au probleme de precizie (0.1 + 0.2 nu dă fix 0.3).
    * **Regulă de aur:** Folosește întotdeauna `BigDecimal` pentru sume financiare.
    * Vei avea nevoie de 3 tabele principale:
        * `User` (id, nume, email, password)
        * `Account` (id, user_id, iban, currency, balance `BigDecimal`)
        * `Transaction` (id, source_account_id, destination_account_id, amount, timestamp, status).

### Faza 2: Logica de Business - "Inima" Aplicației (Zilele 3-5)
Acesta este codul pe care îl vei arăta la interviu.

1.  **Crearea Conturilor:**
    * Fă un endpoint POST `/api/accounts` prin care creezi un cont nou.
    * Implementează o metodă care generează un IBAN random, dar unic.
2.  **Transferul de Bani (MVP):**
    * Endpoint: POST `/api/transfers`.
    * **Logica:**
        1.  Verifici dacă contul sursă are destui bani (`balance.compareTo(amount) >= 0`).
        2.  Scazi suma din sursă.
        3.  Adaugi suma la destinație.
        4.  Salvezi tranzacția în istoric.
    * **Secretul:** Folosește adnotarea `@Transactional` pe metoda din Service. Asta garantează că dacă pasul 3 eșuează (crapă serverul), pasul 2 se anulează automat (rollback).

### Faza 3: Tratarea erorilor și Validare (Ziua 6)
Un junior slab lasă aplicația să dea "Error 500" cu un stacktrace urât. Un junior bun returnează mesaje clare.

1.  **Global Exception Handler:**
    * Folosește `@ControllerAdvice`.
    * Dacă utilizatorul nu are bani, aruncă o excepție custom `InsufficientFundsException` și returnează un cod HTTP 400 (Bad Request) cu un mesaj JSON frumos: `{"error": "Fonduri insuficiente"}`.
2.  **Validare DTO:**
    * Folosește `@Valid` și adnotări precum `@NotNull`, `@Min(0.01)` pe obiectele care vin din frontend (DTOs). Nu vrei ca cineva să transfere o sumă negativă!

### Faza 4: "The Senior Touch" - Concurență și Audit (Zilele 7-9)
Aici transformi proiectul dintr-unul școlar într-unul enterprise.

1.  **Optimistic Locking:**
    * Adaugă un câmp `@Version private Long version;` în entitatea `Account`.
    * **Scenariu:** Două request-uri vin simultan să ia bani din același cont. Hibernate va vedea versiunea și va lăsa doar unul să treacă, aruncând `OptimisticLockException` pentru celălalt. Așa previi furtul de bani.
2.  **Audit:**
    * Nimic nu se șterge într-o bancă. Chiar dacă un transfer eșuează, salvezi o înregistrare în tabela `Transaction` cu statusul `FAILED` și motivul erorii.

### Faza 5: Securitate și Testing (Zilele 10-12)

1.  **Securitate:**
    * Adaugă Spring Security. Inițial poți folosi Basic Auth, dar ideal este să implementezi un flow de Login care returnează un **JWT**.
    * Asigură-te că Userul X nu poate iniția un transfer din contul Userului Y.
2.  **Testare:**
    * Scrie teste unitare cu **JUnit 5 și Mockito** pentru clasa `TransferService`. Testează cazurile limită: transfer cu sold 0, transfer către un cont inexistent etc.

### Faza 6: Documentație și Deploy (Finalul)

1.  **Swagger:** Adaugă dependența `springdoc-openapi-starter-webmvc-ui`. Asta îți generează automat o pagină unde se poate testa API-ul.
2.  **README.md:** Scrie instrucțiuni clare.~~