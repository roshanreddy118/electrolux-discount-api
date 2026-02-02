# Product Discount Manager - Frontend

React frontend for the Product Discount API built with:
- ⚛️ React 18
- ⚡ Vite
- 🎨 Tailwind CSS
- 🎭 shadcn/ui components

## Quick Start

### Development Mode

```bash
# Install dependencies
npm install

# Start dev server (with API proxy)
npm run dev
```

Open http://localhost:5173

The dev server proxies API calls to `http://localhost:8082` (make sure backend is running).

### Production Build

```bash
# Build for production
npm run build

# Deploy to Ktor (builds + copies to static folder)
npm run deploy
```

The `deploy` script:
1. Builds the React app
2. Copies `dist/*` to `../app/src/main/resources/static/`
3. Backend serves it at http://localhost:8082/

## Features

✅ Country selector with VAT rates  
✅ Products grid with discounts  
✅ Apply discount form  
✅ Real-time API status  
✅ Responsive design  
✅ Professional UI with shadcn/ui  
✅ Smooth transitions  

## Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   └── ui/          # shadcn/ui components
│   ├── lib/
│   │   └── utils.js     # Utility functions
│   ├── App.jsx          # Main application
│   ├── main.jsx         # Entry point
│   └── index.css        # Tailwind styles
├── deploy.ps1           # Deployment script
├── package.json
├── vite.config.js
└── tailwind.config.js
```

## API Integration

The app connects to the Ktor backend:

- Development: Proxied through Vite (`/api/*` → `http://localhost:8082/*`)
- Production: Same origin (served by Ktor)

Endpoints used:
- `GET /health` - API status
- `GET /products?country={country}` - List products
- `PUT /products/{id}/discount` - Apply discount

## Deployment

After running `npm run deploy`, the frontend is bundled into the Ktor application:

```
app/src/main/resources/static/
├── index.html
└── assets/
    ├── index-[hash].js
    └── index-[hash].css
```

Run Ktor and access the UI at http://localhost:8082/
