/**
 * Generate a placeholder image as a data URI
 * @param {string} text - Text to display on the placeholder
 * @param {string} bgColor - Background color (hex without #)
 * @param {string} textColor - Text color (hex without #)
 * @param {number} width - Image width
 * @param {number} height - Image height
 * @returns {string} Data URI for the placeholder image
 */
export const generatePlaceholder = (text = 'Movie', bgColor = '667eea', textColor = 'FFFFFF', width = 300, height = 450) => {
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d');

  // Background gradient
  const gradient = ctx.createLinearGradient(0, 0, 0, height);
  gradient.addColorStop(0, `#${bgColor}`);
  gradient.addColorStop(1, `#${adjustBrightness(bgColor, -20)}`);
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, width, height);

  // Text
  ctx.fillStyle = `#${textColor}`;
  ctx.font = 'bold 24px Arial';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  
  // Wrap text if too long
  const maxWidth = width - 40;
  const words = text.split(' ');
  let lines = [];
  let currentLine = words[0];

  for (let i = 1; i < words.length; i++) {
    const word = words[i];
    const testLine = currentLine + ' ' + word;
    const metrics = ctx.measureText(testLine);
    
    if (metrics.width > maxWidth) {
      lines.push(currentLine);
      currentLine = word;
    } else {
      currentLine = testLine;
    }
  }
  lines.push(currentLine);

  // Draw lines
  const lineHeight = 30;
  const startY = (height - (lines.length * lineHeight)) / 2;
  
  lines.forEach((line, index) => {
    ctx.fillText(line, width / 2, startY + (index * lineHeight) + 15);
  });

  // Add decorative film strip border
  ctx.strokeStyle = `#${textColor}`;
  ctx.lineWidth = 8;
  ctx.strokeRect(10, 10, width - 20, height - 20);

  // Add small dots for film strip effect
  ctx.fillStyle = `#${textColor}`;
  for (let i = 20; i < height - 20; i += 30) {
    ctx.fillRect(15, i, 10, 15);
    ctx.fillRect(width - 25, i, 10, 15);
  }

  return canvas.toDataURL('image/png');
};

/**
 * Adjust color brightness
 */
const adjustBrightness = (color, amount) => {
  const num = parseInt(color, 16);
  const r = Math.max(0, Math.min(255, (num >> 16) + amount));
  const g = Math.max(0, Math.min(255, ((num >> 8) & 0x00FF) + amount));
  const b = Math.max(0, Math.min(255, (num & 0x0000FF) + amount));
  return ((r << 16) | (g << 8) | b).toString(16).padStart(6, '0');
};

/**
 * Get a valid image URL with fallback
 * @param {string} url - Original image URL
 * @param {string} fallbackText - Text for fallback image
 * @returns {string} Valid image URL or data URI
 */
export const getValidImageUrl = (url, fallbackText = 'Movie') => {
  // Check if URL is valid
  if (url && 
      url !== 'null' && 
      url !== 'undefined' && 
      !url.includes('null') && 
      !url.includes('undefined') &&
      url.startsWith('http')) {
    return url;
  }
  
  // Return data URI placeholder
  return generatePlaceholder(fallbackText);
};

/**
 * Color palette for different movies
 */
const colorPalette = [
  '667eea', 'FF6B9D', '4ECDC4', 'A8E6CF', 'FFD93D',
  'C7CEEA', 'FF9AA2', '88B04B', 'CC0000', 'B4A7D6',
  'E8B4B8', 'FFB347', '77DD77', 'FFDAB9', 'FFCC5C'
];

/**
 * Get a consistent color for a movie based on its name
 */
export const getMovieColor = (movieName = 'Movie') => {
  const hash = movieName.split('').reduce((acc, char) => {
    return char.charCodeAt(0) + ((acc << 5) - acc);
  }, 0);
  
  const index = Math.abs(hash) % colorPalette.length;
  return colorPalette[index];
};

/**
 * Create a movie placeholder with consistent color
 */
export const createMoviePlaceholder = (movieName, width = 300, height = 450) => {
  const color = getMovieColor(movieName);
  return generatePlaceholder(movieName, color, 'FFFFFF', width, height);
};
