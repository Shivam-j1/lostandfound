# Campus Lost & Found Portal
Spring Boot 3 + Spring Data JPA + H2 (SQL) + accessible HTML/CSS/JS frontend.

## Features
**Backend (server side)**
1. REST API: report items, search by keyword/location, filter by type (SQL via JPA) with server-side validation (Bean Validation) and clean JSON errors.
2. Anti-theft claim verification: secret answer is never sent to the browser; 3 wrong attempts lock the item.
3. Extra: `/api/items/stats` SQL COUNT dashboard data; demo data seeder.

**Frontend (client side)**
1. Live debounced search + filter, summary dashboard, dynamic rendering.
2. Accessible client-side validation: inline errors, `aria-invalid`, error summary with focus management, skip link, WCAG 2.1 AA contrast.

## Run
1. Open this folder in IntelliJ IDEA (File > Open > select `lostandfound`), wait for Maven import.
2. Run `LostAndFoundApplication.java`.
3. Site: http://localhost:8080
4. DB console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/lostfounddb`, user `sa`, blank password)

## API
| Method | URL | Purpose |
|---|---|---|
| GET | /api/items?search=&type= | List / search / filter |
| POST | /api/items | Report an item |
| POST | /api/items/{id}/claim | Verify ownership answer |
| GET | /api/items/stats | Dashboard counts |
