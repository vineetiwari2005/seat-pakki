# BookMyShow Frontend

A modern, production-ready React frontend for the BookMyShow application with comprehensive features including movie booking, seat selection, payment processing, and admin/theater owner dashboards.

## 🌟 Features

### User Features
- **Authentication & Authorization** - JWT-based secure login/signup with role-based access
- **Movie Browsing** - Search and filter movies by genre, language, rating, and city
- **Booking Flow** - Select showtimes → Choose seats → Make payment → Get confirmation
- **My Bookings** - View booking history and cancel tickets with refunds
- **Profile Management** - Update profile and check wallet balance

### Admin Panel
- **Dashboard** - Analytics, revenue reports, and booking statistics
- **User Management** - Activate/deactivate users
- **Content Management** - Manage movies, theaters, and shows
- **Analytics** - View trends and popular movies

### Theater Owner Panel
- **Theater Management** - Manage owned theaters and screens
- **Show Management** - Add and schedule shows
- **Revenue Reports** - Track theater performance

### Technical Highlights
- **Modern UI/UX** - BookMyShow-inspired design with smooth animations
- **Responsive Design** - Mobile-first approach, works on all devices
- **Real-time Updates** - Seat locking and availability updates
- **Mock Data** - Pre-loaded Indian cities, theaters, and movies
- **Secure** - JWT token management with auto-refresh

## 📋 Prerequisites

Before running the frontend, ensure you have:

- **Node.js** 16+ and npm/yarn installed
- **Backend API** running on `http://localhost:8080`
- Modern web browser (Chrome, Firefox, Safari, Edge)

## 🚀 Quick Start

### 1. Navigate to Frontend Directory

```bash
cd Book-My-Show/frontend
```

### 2. Install Dependencies

```bash
npm install
```

### 3. Start Development Server

```bash
npm run dev
```

The application will start on `http://localhost:3000`

### 4. Build for Production

```bash
npm run build
```

Production files will be in the `dist` directory.

### 5. Preview Production Build

```bash
npm run preview
```

## 📁 Project Structure

```
frontend/
├── public/                 # Static assets
├── src/
│   ├── components/        # Reusable components
│   │   ├── Layout/       # Navbar, Footer
│   │   ├── Movie/        # MovieCard, FilterBar
│   │   ├── Booking/      # SeatMap, PaymentForm
│   │   └── Admin/        # Admin components
│   ├── pages/            # Page components
│   │   ├── Home/         # Home page
│   │   ├── Auth/         # Login, Signup
│   │   ├── Movie/        # MovieDetails
│   │   ├── Booking/      # Booking flow pages
│   │   ├── User/         # Profile, MyBookings
│   │   ├── Admin/        # Admin dashboard
│   │   └── TheaterOwner/ # Theater owner dashboard
│   ├── services/         # API service layer
│   │   ├── api.js        # Axios instance with interceptors
│   │   └── index.js      # All service functions
│   ├── context/          # React Context
│   │   ├── AuthContext.jsx    # Authentication state
│   │   └── AppContext.jsx     # App-wide state
│   ├── mockData/         # Mock data for India
│   │   └── indianData.js # Cities, theaters, movies
│   ├── styles/           # Global styles
│   │   └── global.scss   # Base styles and utilities
│   ├── App.jsx           # Main app component with routing
│   └── main.jsx          # Entry point
├── index.html            # HTML template
├── vite.config.js        # Vite configuration
└── package.json          # Dependencies
```

## 🎨 Design System

### Color Palette
- **Primary**: `#f84464` (BookMyShow Red)
- **Secondary**: `#333545` (Dark Gray)
- **Accent**: `#ff6f61` (Coral)
- **Success**: `#27ae60`
- **Warning**: `#f39c12`
- **Error**: `#e74c3c`

### Typography
- **Primary Font**: Poppins
- **Secondary Font**: Roboto

### Breakpoints
- **Mobile**: < 480px
- **Tablet**: 768px
- **Desktop**: 1024px
- **Large Desktop**: 1400px

## 🔌 API Integration

The frontend communicates with the backend via REST APIs:

### Base URL
```javascript
http://localhost:8080
```

### Authentication Headers
```javascript
Authorization: Bearer <JWT_TOKEN>
```

### Key Endpoints Used
- `POST /api/auth/login` - User login
- `POST /api/auth/signup` - User registration
- `GET /movie/get-all-movies` - Get all movies
- `POST /ticket/book-ticket` - Book tickets
- `POST /api/payments/initiate` - Initiate payment
- `GET /admin/dashboard` - Admin dashboard data

See `src/services/index.js` for complete API documentation.

## 🗺️ Mock Data

The application includes realistic Indian mock data:

### Cities (7)
Mumbai, Delhi, Bangalore, Chennai, Kolkata, Hyderabad, Pune

### Theaters (35 total, 5-10 per city)
- **Mumbai**: PVR Phoenix Palladium, INOX Nariman Point, Cinepolis Andheri, etc.
- **Delhi**: PVR Select Citywalk, INOX Nehru Place, Cinepolis DLF Place, etc.
- **Bangalore**: PVR Forum Mall, INOX Garuda Mall, etc.

### Movies (20 current Bollywood/Regional)
Jawan, Pathaan, RRR, KGF Chapter 2, Brahmastra, Vikram, Drishyam 2, Ponniyin Selvan, Kantara, Animal, Dunki, Salaar, Tiger 3, etc.

### Showtimes
10:00 AM, 1:30 PM, 4:00 PM, 7:00 PM, 10:00 PM

### Seat Layout
5 rows (A-E) × 10 seats per row = 50 seats per screen

## 🔐 User Roles

### USER (Default)
- Browse and search movies
- Book tickets
- Manage bookings
- View profile

### ADMIN
- All user features
- Admin dashboard access
- Manage users (activate/deactivate)
- Manage movies, theaters, shows
- View analytics

### THEATER_OWNER
- All user features
- Manage owned theaters
- Add screens and shows
- View revenue reports

## 🧪 Testing the Application

### Test User Accounts

After backend is running, create test accounts:

```javascript
// Regular User
{
  "name": "Test User",
  "email": "user@test.com",
  "password": "password123",
  "mobileNumber": "9876543210",
  "age": 25,
  "gender": "MALE"
}

// Admin (needs to be set in database)
{
  "email": "admin@bookmyshow.com",
  "password": "admin123",
  "role": "ADMIN"
}
```

### Testing Flow

1. **Signup/Login** - Create account or login
2. **Browse Movies** - View movies in selected city
3. **Filter** - Try genre, language, rating filters
4. **Movie Details** - Click on any movie
5. **Select Show** - Choose theater, date, and time
6. **Select Seats** - Pick seats (10-minute lock)
7. **Payment** - Enter payment details (mock gateway)
8. **Confirmation** - View booking confirmation
9. **My Bookings** - See all bookings, cancel if needed

## 🛠️ Development

### Available Scripts

```bash
# Start dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

### Environment Variables

Create `.env` file if needed:

```env
VITE_API_BASE_URL=http://localhost:8080
```

### Adding New Features

1. Create component in appropriate directory
2. Add routing in `App.jsx`
3. Create corresponding SCSS file
4. Add API service functions if needed
5. Update mock data if required

## 📱 Mobile Optimization

The application is fully responsive:

- **Hamburger Menu** - Mobile navigation
- **Touch Optimized** - Seat selection, filters
- **Optimized Images** - Lazy loading for posters
- **Flexible Layouts** - Grid system adapts to screen size

## 🎯 Best Practices Implemented

- ✅ Component-based architecture
- ✅ Context API for state management
- ✅ Custom hooks for reusability
- ✅ Protected routes for authentication
- ✅ Error boundaries and error handling
- ✅ Loading states and skeletons
- ✅ Toast notifications for user feedback
- ✅ Accessibility (ARIA labels, keyboard navigation)
- ✅ SEO-friendly meta tags
- ✅ Performance optimizations (lazy loading, memoization)

## 🐛 Troubleshooting

### Backend Connection Issues
```bash
# Check if backend is running
curl http://localhost:8080/movie/get-all-movies

# Verify proxy configuration in vite.config.js
```

### CORS Errors
Ensure backend has CORS enabled for `http://localhost:3000`

### Token Expiration
Tokens auto-refresh. If issues persist, clear localStorage and re-login.

### Build Errors
```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

## 📦 Dependencies

### Core
- **React** 18.2.0 - UI library
- **React Router** 6.20.0 - Routing
- **Axios** 1.6.2 - HTTP client

### UI & Styling
- **Sass** 1.69.5 - CSS preprocessor
- **React Icons** 4.12.0 - Icon library
- **React Toastify** 9.1.3 - Notifications

### Utils
- **date-fns** 2.30.0 - Date formatting

### Dev Tools
- **Vite** 5.0.8 - Build tool
- **@vitejs/plugin-react** 4.2.1 - React plugin

## 🚀 Deployment

### Deploy to Netlify/Vercel

1. Build the project:
```bash
npm run build
```

2. Deploy `dist` folder

3. Set environment variables in hosting platform

### Deploy with Docker

```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
RUN npm install -g serve
CMD ["serve", "-s", "dist", "-l", "3000"]
EXPOSE 3000
```

## 📄 License

This project is part of the BookMyShow application.

## 👥 Support

For issues or questions:
- Check existing documentation
- Review API endpoints in backend
- Check browser console for errors
- Verify backend is running correctly

---

**Built with ❤️ using React + Vite**
