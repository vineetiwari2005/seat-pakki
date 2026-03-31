#!/bin/bash

# BookMyShow Frontend Diagnostic Script
# This script checks if your frontend setup is correct

echo "🔍 BookMyShow Frontend Diagnostic Tool"
echo "========================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Navigate to frontend directory
cd "$(dirname "$0")/frontend" || exit 1

echo "📁 Current directory: $(pwd)"
echo ""

# Check 1: Node.js version
echo "1️⃣  Checking Node.js..."
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    echo -e "${GREEN}✅ Node.js installed: $NODE_VERSION${NC}"
else
    echo -e "${RED}❌ Node.js not found! Install from https://nodejs.org${NC}"
    exit 1
fi
echo ""

# Check 2: npm version
echo "2️⃣  Checking npm..."
if command -v npm &> /dev/null; then
    NPM_VERSION=$(npm --version)
    echo -e "${GREEN}✅ npm installed: $NPM_VERSION${NC}"
else
    echo -e "${RED}❌ npm not found!${NC}"
    exit 1
fi
echo ""

# Check 3: Critical files
echo "3️⃣  Checking critical files..."
FILES=(
    "package.json"
    "index.html"
    "src/main.jsx"
    "src/App.jsx"
    "src/mockData/indianData.js"
    "src/context/AuthContext.jsx"
    "src/context/AppContext.jsx"
    "src/pages/Home/Home.jsx"
    "src/components/Layout/Navbar.jsx"
    "src/services/index.js"
)

MISSING_COUNT=0
for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "${GREEN}✅${NC} $file"
    else
        echo -e "${RED}❌ MISSING:${NC} $file"
        ((MISSING_COUNT++))
    fi
done
echo ""

if [ $MISSING_COUNT -gt 0 ]; then
    echo -e "${RED}⚠️  $MISSING_COUNT critical files missing!${NC}"
    echo ""
fi

# Check 4: node_modules
echo "4️⃣  Checking node_modules..."
if [ -d "node_modules" ]; then
    echo -e "${GREEN}✅ node_modules directory exists${NC}"
    
    # Check key packages
    KEY_PACKAGES=("react" "react-dom" "react-router-dom" "vite" "axios")
    for pkg in "${KEY_PACKAGES[@]}"; do
        if [ -d "node_modules/$pkg" ]; then
            echo -e "${GREEN}  ✅${NC} $pkg installed"
        else
            echo -e "${YELLOW}  ⚠️${NC}  $pkg not found (run npm install)"
        fi
    done
else
    echo -e "${YELLOW}⚠️  node_modules not found${NC}"
    echo -e "${YELLOW}   Run: npm install${NC}"
fi
echo ""

# Check 5: File counts
echo "5️⃣  Checking project structure..."
JSX_COUNT=$(find src -name "*.jsx" 2>/dev/null | wc -l | xargs)
SCSS_COUNT=$(find src -name "*.scss" 2>/dev/null | wc -l | xargs)

echo "   📄 JSX files: $JSX_COUNT (expected: ~40+)"
echo "   🎨 SCSS files: $SCSS_COUNT (expected: ~15+)"
echo ""

# Check 6: Port availability
echo "6️⃣  Checking port availability..."
if lsof -Pi :3000 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Port 3000 is in use (Vite will use 3001)${NC}"
else
    echo -e "${GREEN}✅ Port 3000 is available${NC}"
fi

if lsof -Pi :3001 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Port 3001 is in use (Vite will use 3002)${NC}"
else
    echo -e "${GREEN}✅ Port 3001 is available${NC}"
fi
echo ""

# Check 7: Backend availability (optional)
echo "7️⃣  Checking backend availability..."
if curl -s http://localhost:8080/movie/get-all-movies >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Backend is running on port 8080${NC}"
else
    echo -e "${YELLOW}⚠️  Backend not responding on port 8080${NC}"
    echo -e "${YELLOW}   (This is OK for frontend-only testing)${NC}"
fi
echo ""

# Summary
echo "========================================"
echo "📊 SUMMARY"
echo "========================================"

if [ $MISSING_COUNT -eq 0 ] && [ -d "node_modules" ]; then
    echo -e "${GREEN}✅ All checks passed! You're ready to run the app.${NC}"
    echo ""
    echo "🚀 To start the development server:"
    echo "   npm run dev"
    echo ""
    echo "Then open the URL shown in terminal (usually http://localhost:3001)"
    echo ""
    echo "🧪 To test if React is loading:"
    echo "   Open: http://localhost:3001/test"
    echo ""
    echo "🐛 If you see a blank page:"
    echo "   1. Open browser console (F12)"
    echo "   2. Check for error messages (red text)"
    echo "   3. See TROUBLESHOOTING.md for detailed help"
elif [ $MISSING_COUNT -gt 0 ]; then
    echo -e "${RED}❌ Setup incomplete - $MISSING_COUNT files missing${NC}"
    echo ""
    echo "📝 Action required:"
    echo "   Review the missing files above"
    echo "   Check if files were created correctly"
elif [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}⚠️  Dependencies not installed${NC}"
    echo ""
    echo "📝 Action required:"
    echo "   Run: npm install"
    echo "   Then: npm run dev"
else
    echo -e "${YELLOW}⚠️  Some issues detected${NC}"
    echo ""
    echo "📝 Review the warnings above"
fi

echo ""
echo "📚 Documentation:"
echo "   - QUICK_REFERENCE.md - Quick commands and tips"
echo "   - TROUBLESHOOTING.md - Detailed troubleshooting guide"
echo "   - GETTING_STARTED.md - Complete setup guide"
echo ""
