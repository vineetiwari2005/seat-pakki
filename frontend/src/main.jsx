import React from 'react';
import ReactDOM from 'react-dom/client';

console.log('🚀 SeatPakki Frontend Starting...');
console.log('React:', React);
console.log('ReactDOM:', ReactDOM);

const root = document.getElementById('root');
console.log('Root element:', root);

if (!root) {
  console.error('❌ Root element not found!');
  document.body.innerHTML = '<h1 style="color:red;padding:50px;">ERROR: Root element not found!</h1>';
} else {
  console.log('✅ Root element found, attempting to import App...');
  
  import('./App').then((AppModule) => {
    console.log('✅ App imported successfully:', AppModule);
    const App = AppModule.default;
    
    console.log('✅ Creating React root...');
    const reactRoot = ReactDOM.createRoot(root);
    console.log('✅ React root created, rendering App...');
    
    reactRoot.render(
      <React.StrictMode>
        <App />
      </React.StrictMode>
    );
    console.log('✅ App rendered successfully!');
  }).catch((error) => {
    console.error('❌ Failed to import or render App:', error);
    root.innerHTML = `
      <div style="padding: 50px; font-family: Arial; background: #fee; color: #c00;">
        <h1>Failed to Load Application</h1>
        <p><strong>Error:</strong> ${error.message}</p>
        <pre style="background: #fff; padding: 15px; overflow: auto;">${error.stack}</pre>
        <p>Check browser console (F12) for more details.</p>
      </div>
    `;
  });
}
