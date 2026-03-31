#!/bin/bash

# BookMyShow Frontend Setup Script

echo "🎬 BookMyShow Frontend Setup"
echo "=============================="
echo ""

# Check Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js 16+ first."
    exit 1
fi

echo "✅ Node.js version: $(node --version)"

# Check npm
if ! command -v npm &> /dev/null; then
    echo "❌ npm is not installed."
    exit 1
fi

echo "✅ npm version: $(npm --version)"
echo ""

# Navigate to frontend directory
cd "$(dirname "$0")"

echo "📦 Installing dependencies..."
npm install

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Dependencies installed successfully!"
    echo ""
    echo "🚀 You can now:"
    echo "   • Start development server: npm run dev"
    echo "   • Build for production: npm run build"
    echo ""
    echo "📝 Make sure your backend is running on http://localhost:8080"
    echo ""
    echo "🌐 Frontend will be available at: http://localhost:3000"
    echo ""
else
    echo "❌ Failed to install dependencies"
    exit 1
fi
