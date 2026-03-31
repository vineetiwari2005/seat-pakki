import React, { createContext, useState, useContext, useEffect } from 'react';
import { publicService } from '../services';

// Fallback cities used only if DB fetch fails
const FALLBACK_CITIES = [
  { id: 1, name: 'Mumbai' },
  { id: 2, name: 'Delhi' },
  { id: 3, name: 'Bangalore' },
  { id: 4, name: 'Chennai' },
  { id: 5, name: 'Kolkata' },
  { id: 6, name: 'Hyderabad' },
  { id: 7, name: 'Pune' },
];

const AppContext = createContext(null);

export const AppProvider = ({ children }) => {
  const [selectedCity, setSelectedCity] = useState(() => {
    return localStorage.getItem('selectedCity') || 'Mumbai';
  });
  
  const [searchQuery, setSearchQuery] = useState('');
  const [cities, setCities] = useState(FALLBACK_CITIES);
  const [filters, setFilters] = useState({
    genre: '',
    language: '',
    minRating: 0
  });

  // Fetch cities from DB on mount and when window regains focus
  const fetchCitiesFromDB = async () => {
    try {
      const data = await publicService.getCities();
      if (Array.isArray(data) && data.length > 0) {
        setCities(data);
        // Auto-select user's city if logged in
        try {
          const storedUser = JSON.parse(localStorage.getItem('user'));
          if (storedUser && storedUser.cityId) {
            const userCity = data.find(c => c.id === storedUser.cityId);
            if (userCity) {
              setSelectedCity(userCity.name);
            }
          }
        } catch (e) {}
      }
    } catch (err) {
      console.warn('Could not fetch cities from DB, using fallback list');
    }
  };

  useEffect(() => {
    fetchCitiesFromDB();
    // Re-fetch when window regains focus (catches admin adding new cities in another tab)
    const handleFocus = () => fetchCitiesFromDB();
    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, []);

  useEffect(() => {
    localStorage.setItem('selectedCity', selectedCity);
  }, [selectedCity]);

  const changeCity = (city) => {
    setSelectedCity(city);
  };

  const updateFilters = (newFilters) => {
    setFilters({ ...filters, ...newFilters });
  };

  const resetFilters = () => {
    setFilters({
      genre: '',
      language: '',
      minRating: 0
    });
    setSearchQuery('');
  };

  const value = {
    selectedCity,
    changeCity,
    searchQuery,
    setSearchQuery,
    filters,
    updateFilters,
    resetFilters,
    cities,
  };

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};

export default AppContext;
