# J-Vault - Core Banking API

J-Vault este o aplicație backend de tip "Core Banking" robustă și scalabilă, construită pentru a simula operațiuni financiare critice într-un mediu securizat.

## 🚀 Caracteristici Principale

* **Gestiunea Conturilor:** Creare de conturi multi-valută (RON, EUR, USD) cu generare automată de IBAN unic.
* **Tranzacții Atomice:** Transferuri de bani sigure folosind tranzacții ACID.
* **Concurență (Optimistic Locking):** Previne suprascrierea datelor când două tranzacții au loc simultan (`@Version`).
* **Audit Complet:**
    * Audit Financiar: Tranzacțiile eșuate sunt salvate în baza de date cu motivul erorii.
    * Audit de Securitate: Logarea IP-ului și a acțiunilor sensibile (Login, Change Password).
* **Securitate:** Autentificare bazată pe **JWT (JSON Web Tokens)** + Refresh Token mechanism.
* **Documentație:** Swagger UI integrat.

## 🛠️ Tehnologii Folosite

* **Java 17** & **Spring Boot 3**
* **Spring Data JPA** (Hibernate) & **PostgreSQL**
* **Spring Security** & **JJWT**
* **Docker** & **Docker Compose**
* **JUnit 5** & **Mockito** (Unit Testing)
* **Lombok** & **Maven**

## 🏁 Cum să rulezi aplicația

1.  **Clonează repository-ul:**
    ```bash
    git clone [https://github.com/gabi2005orzoi/jvault.git](https://github.com/gabi2005orzoi/jvault.git)
    cd jvault
    ```

2.  **Pornește baza de date (Docker):**
    ```bash
    docker-compose up -d
    ```

3.  **Pornește aplicația:**
    ```bash
    ./mvnw spring-boot:run
    ```

4.  **Testează API-ul:**
    Accesează Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## 🧪 Testare

Rulează suita de teste unitare:
```bash
./mvnw test

