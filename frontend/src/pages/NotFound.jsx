import React from 'react';
import './NotFound.scss';

const NotFound = () => {
  return (
    <div className="not-found-page">
      <div className="container">
        <div className="not-found-content">
          <h1>404</h1>
          <h2>Page Not Found</h2>
          <p>The page you're looking for doesn't exist or has been moved.</p>
          <a href="/" className="btn btn-primary">
            Go to Homepage
          </a>
        </div>
      </div>
    </div>
  );
};

export default NotFound;
