# Smoke Runbook (Front/Back) after #22 / #29 / #27 / #62

## Scope
- `#22` (front): spacer in article/publication sections
- `#29` (back): `[ADMIN]` labels for mutating API methods in Swagger/OpenAPI
- `#27` (back): program description document in GOST format
- `#62` (front): smoke runbook for clickability and routes (header/projects/footer/legal)

## 1) Start services
```bash
# Terminal 1: infrastructure
cd /Users/egorkuznecov/Documents/Документы/NSU/Subjects/poprog/poprog-knowledge-base-back
docker compose up -d

# Terminal 2: backend
cd /Users/egorkuznecov/Documents/Документы/NSU/Subjects/poprog/poprog-knowledge-base-back
# If 8080 is busy locally, run on 18080.
SEARCH_ENABLED=false SPRING_PROFILES_ACTIVE=local SERVER_PORT=18080 ./gradlew bootRun

# Terminal 3: frontend
cd /Users/egorkuznecov/Documents/Документы/NSU/Subjects/poprog/poprog-knowledge-base-front
npm install
VITE_API_BASE_URL=http://localhost:18080 npm run dev
```

Expected:
- Front is available at `http://localhost:5173`
- Back is available at `http://localhost:18080`
- Swagger UI is available at `http://localhost:18080/swagger-ui/index.html`

## 2) API smoke (quick)
```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:5173/
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:18080/swagger-ui/index.html
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:18080/api/publications/grouped
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:18080/api/student-works/grouped
curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:18080/api/search?q=prog&limit=5"
```

Expected: `200` for all requests.

## 3) Front smoke for #22 (spacer)
1. Open `http://localhost:5173/publications`.
2. Check that publication blocks are visually separated (no "glued" cards/rows).
3. Open one projects article page, for example `http://localhost:5173/projects/success-stories`.
4. Check spacing between article sections/blocks.
5. Open home page and click active case card arrow in "Внедрение новых решений...".
6. Verify route changes to `/projects/:itemSlug` and page is rendered (no fallback/blank screen).

Expected: stable vertical spacing between neighboring blocks on desktop and mobile viewport.

## 4) Front click-path smoke for #62 (buttons/routes/legal/projects panel)
1. Open `http://localhost:5173/home`.
2. Header:
   - click `Вход в консоль`, `Создать аккаунт`, and mobile account icon;
   - expected: external navigation warning modal opens, `Продолжить` opens external page in new tab.
3. Projects panel (desktop):
   - click top nav `Проекты`, verify panel opens (`Панель проектов`);
   - switch at least 2 categories in left sidebar;
   - click panel close button and then reopen and close via backdrop;
   - expected: panel opens/closes reliably, no double-active nav state.
4. Projects panel (mobile):
   - switch viewport to mobile width;
   - open `Меню` -> `Проекты` -> category -> item;
   - expected: route opens `/projects/:itemSlug`, menu/panel state resets after navigation.
5. Footer:
   - click `Создать аккаунт RIDE`, expected external warning modal and working continuation;
   - click legal links `Конфиденциальность`, `Условия пользования сайтом`, `Параметры файлов cookie`;
   - expected: URL hash changes to `#privacy`, `#terms`, `#cookies`, page remains usable, no 404.

## 5) Swagger/OpenAPI smoke for #29 (`[ADMIN]`)
### UI check
1. Open `http://localhost:8080/swagger-ui/index.html`.
1. (If you used port 18080) Open `http://localhost:18080/swagger-ui/index.html`.
2. Go to "Меню проектов".
3. Verify that all mutating endpoints have `[ADMIN]` prefix in summary:
   - `POST /api/projects/menu/sections`
   - `PUT /api/projects/menu/sections/{id}`
   - `DELETE /api/projects/menu/sections/{id}`
   - `POST /api/projects/menu/items`
   - `PUT /api/projects/menu/items/{id}`
   - `DELETE /api/projects/menu/items/{id}`
   - `POST /api/projects/menu/promos`
   - `PUT /api/projects/menu/promos/{id}`
   - `DELETE /api/projects/menu/promos/{id}`
   - `POST /api/projects/menu/resources/upload`

### Raw OpenAPI check
```bash
curl -s http://localhost:8080/v3/api-docs | rg "\"summary\"\\s*:\\s*\"\\[ADMIN\\]"
```

Expected: multiple matches for all mutating menu endpoints listed above.

## 6) Docs smoke for #27
```bash
test -f /Users/egorkuznecov/Documents/Документы/NSU/Subjects/poprog/poprog-knowledge-base-back/docs/program-description-gost.md && echo OK
rg -n "ГОСТ 19.402-78|ГОСТ 19.105-78|Титульный лист" /Users/egorkuznecov/Documents/Документы/NSU/Subjects/poprog/poprog-knowledge-base-back/docs/program-description-gost.md
```

Expected:
- file exists
- includes GOST references and title page section.

## 7) Shutdown
```bash
cd /Users/egorkuznecov/Documents/Документы/NSU/Subjects/poprog/poprog-knowledge-base-back
docker compose down
```
