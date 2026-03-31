# Memory Game (Matiks-Style) Implementation Guide

## Overview
This document describes the implementation of a Matiks-style memory game with an integrated reward system using cashback/temporary wallet.

## Game Mechanics

### Core Concept
The game is a **fast-paced spatial memory test** that challenges players to:
1. Watch a pattern of tiles light up (3 seconds)
2. Remember the exact positions
3. Recreate the pattern by clicking tiles
4. Progress through increasing difficulty levels
5. All within a strict 2-minute countdown timer

### Gameplay Loop

#### 1. The Grid Appears
- Game starts with a blank grid (e.g., 3x3)
- Grid is displayed with square tiles in black color
- No tiles are clickable during flash phase

#### 2. The Flash (Memorization Phase)
- Random tiles suddenly **light up in bright blue** color
- Tiles remain lit for exactly **3 seconds**
- Player cannot click anything during this phase
- Player must memorize the exact positions of lit tiles

#### 3. The Recall (Action Phase)
- After 3 seconds, all tiles disappear (turn blank/black again)
- Grid goes completely blank
- **Player must now click the exact tiles** that were previously highlighted
- Tiles are clickable only during this phase

#### 4. The Outcome (Success or Failure)

**If Correct:**
- Once player clicks all correct tiles: Grid flashes green
- Player earns **points** (tiles_to_click × 10)
- Game immediately jumps to the next round
- No lives lost

**If Incorrect:**
- The moment player clicks a wrong tile:
  - Grid shakes and flashes red
  - Correct pattern is revealed momentarily
  - Player loses **1 life**
  - Round resets after delay

#### 5. Difficulty Progression
Difficulty increases automatically after each successful round:

- **Level 1:** 3×3 grid, 3 tiles light up
- **Level 2:** 3×3 grid, 4 tiles light up
- **Level 3:** 3×3 grid, 5 tiles light up
- **Level 4:** 4×4 grid, 5 tiles light up
- **Level 5:** 4×4 grid, 6 tiles light up
- And so on...

The game becomes progressively harder as grid size increases and more tiles need to be remembered.

#### 6. Game End
- Game **freezes exactly at 2 minutes** (120 seconds)
- Timer counts down from 120 to 0
- Final score is calculated based on:
  - Number of rounds successfully completed
  - Highest difficulty level reached

### Tile States
- **Black (Default):** Inactive tile, not yet lit
- **Blue (Lit):** Tile is highlighted during flash phase
- **Highlighted on Click:** Shows visual feedback when clicked

## Reward System

### Reward Tiers

Based on the user's final score, calculate reward as follows:

#### Tier 1: Highest Score of Month
```
If userScore >= highest_score_of_month AND highest_score_of_month > 0
→ Award ₹20 (Maximum reward)
```

#### Tier 2: Well Above Average
```
If userScore > average_score × 1.5
→ Award random between ₹9 to ₹10
```

#### Tier 3: Above/At Average
```
If userScore >= average_score
→ Award random between ₹2 to ₹3
```

#### Tier 4: Below Average
```
If userScore < average_score
→ Award ₹0 (No reward)
```

### The "Addiction" Factor (Variable Ratio Reinforcement)
- **30% chance** of receiving **₹0 reward**, even if score is good
- This creates unpredictability that keeps users coming back
- This is called "Variable Ratio Reinforcement" (same mechanism as slot machines)

### Daily Play Restriction
- User can play **only ONCE per day**
- Tracked by `DailyGameLogs` table with `(user_id, played_date)` unique constraint
- System rejects duplicate attempts with error message

### Temporary Wallet Details
- Reward is stored in `TemporaryWallet`
- **Valid for exactly 10 days** from earning date
- After 10 days:
  - `is_expired` flag is set to TRUE
  - Reward becomes unusable
  - User loses the reward

### Reward Calculation Example

**Scenario 1:** User scores 850, monthly average is 500, highest is 800
- Score (850) >= Highest (800) ✓
- **Reward: ₹20**

**Scenario 2:** User scores 850, monthly average is 500, highest is 900
- Score (850) > Average × 1.5 (750) ✓
- **Reward: ₹9-₹10** (random)

**Scenario 3:** User scores 600, monthly average is 500, highest is 900
- Score (600) >= Average (500) ✓
- **Reward: ₹2-₹3** (random)

**Scenario 4:** User scores 400, monthly average is 500
- Score (400) < Average (500) ✗
- **Reward: ₹0**

**Scenario 5:** User scores 900 (good score), but...
- 30% luck factor triggers
- **Reward: ₹0** (even with good score)

## Database Schema

### DailyGameLogs
```sql
- id (PK)
- user_id (FK)
- score
- played_date
- created_at
- UNIQUE(user_id, played_date) ← Ensures one play per day
```

### TemporaryWallet
```sql
- id (PK)
- user_id (FK)
- amount (Decimal)
- earned_at
- expires_at ← Current timestamp + 10 days
- is_expired (Boolean)
- is_used (Boolean)
- used_at (Nullable)
```

### GameLeaderboard
```sql
- id (PK)
- month_year (YYYY-MM format)
- user_id (FK)
- highest_score
- total_plays
- average_score
- UNIQUE(user_id, month_year)
```

### MonthlyGameStats
```sql
- id (PK)
- month_year (YYYY-MM format) UNIQUE
- highest_score (aggregated)
- average_score (aggregated)
- total_players
```

## API Endpoints

### 1. Submit Game Score
```
POST /api/game/submit-score

Request Body:
{
  "userId": 123,
  "score": 850,
  "levelReached": 5,
  "gridSizeReached": 4
}

Response:
{
  "rewardId": 456,
  "rewardAmount": 20.00,
  "message": "Congratulations! You won ₹20. It expires in 10 days.",
  "isReward": true,
  "expiresAt": "2024-04-05T10:00:00"
}

Or (if already played today):
{
  "error": "You have already played your daily game. Come back tomorrow!"
}

Or (if bad luck):
{
  "rewardAmount": 0.00,
  "message": "Better luck next time! No reward today.",
  "isReward": false
}
```

### 2. Get User's Active Rewards
```
GET /api/game/user/{userId}/rewards

Response:
{
  "rewards": [
    {
      "id": 1,
      "userId": 123,
      "amount": 20.00,
      "earnedAt": "2024-03-25T10:00:00",
      "expiresAt": "2024-04-04T10:00:00",
      "isExpired": false,
      "isUsed": false
    }
  ],
  "count": 1
}
```

### 3. Get Total Active Rewards
```
GET /api/game/user/{userId}/total-rewards

Response:
{
  "totalAmount": 35.50
}
```

### 4. Mark Expired Rewards
```
POST /api/game/mark-expired

Response:
{
  "message": "Expired rewards marked successfully"
}
```

### 5. Use Reward
```
POST /api/game/use-reward/{rewardId}

Response:
{
  "message": "Reward used successfully"
}
```

## Frontend Component Structure

### Pages
- `pages/Game/Game.jsx` - Main game page wrapper
- `pages/Game/Game.scss` - Page styling

### Components
- `components/Game/MemoryGame.jsx` - Game logic and UI
- `components/Game/MemoryGame.scss` - Game styling

### State Management
- Game state: `intro`, `playing`, `gameOver`
- Phase: `flash` (memorization), `recall` (player action)
- Score tracking, timer, lives, difficulty levels

### Key Features
- 2-minute countdown timer
- Progressive difficulty (grid size and tile count)
- Visual feedback (tiles light up, grid shake on error)
- Score calculation
- UI for showing stats during game and results after

## Integration Points

### Home Page
- Added "Play & Earn" button in game promo section
- Button navigates to `/game` route

### Routing
- `/game` route added to `App.jsx`
- Public route (can be protected if needed)

### Backend Integration
- `GameService` handles all business logic
- `GameController` exposes REST API endpoints
- `DailyGameLogRepository`, `TemporaryWalletRepository`, etc. handle data persistence

## Business Logic Highlights

### Score Submission Flow
1. User submits score after game ends
2. System checks if user already played today
3. Fetches current month's stats (highest score, average)
4. Logs the game in DailyGameLogs
5. Updates GameLeaderboard
6. Applies 30% bad luck factor
7. Calculates reward based on tiers
8. Stores reward in TemporaryWallet with 10-day expiration
9. Returns reward response to frontend

### Unique Features
- **Variable Ratio Reinforcement:** 30% chance of no reward creates addiction
- **Dynamic Difficulty:** Grid and tile count increase automatically
- **10-Day Expiration:** Creates urgency to use rewards
- **Monthly Tracking:** Encourages repeated plays throughout month
- **One-Play-Per-Day:** Limits abuse while encouraging daily engagement

## Testing Scenarios

### Scenario 1: First-Time Player
1. No monthly stats exist
2. Score of 300 → Gets no reward (below average of 0)
3. Entry created in GameLeaderboard
4. Entry created in MonthlyGameStats

### Scenario 2: Repeat Player
1. Monthly stats already exist
2. Score of 600, average is 500
3. If score >= average → ₹2-₹3 reward
4. Entry in TemporaryWallet created with 10-day expiration

### Scenario 3: Lucky Player
1. Score of 900, highest is 850
2. Score >= highest → Could get ₹20
3. But 30% luck check → Gets ₹0 instead

### Scenario 4: Unlucky Player
1. Score of 850, highest is 800
2. 70% luck check passes
3. Score >= highest → Gets ₹20

## Maintenance Tasks

### Daily
- None (expiration is automatic with database timestamps)

### Weekly
- Monitor reward redemption rates
- Check game leaderboard accuracy

### Monthly
- Archive old monthly stats
- Analyze user engagement metrics
- Adjust reward tiers if needed

## Security Considerations

- User ID must be authenticated (via JWT in Authorization header)
- One-play-per-day is database-enforced (unique constraint)
- Score is accepted from frontend but should include validation
- Expired rewards become unusable (checked in queries)
- All API endpoints should require authentication

## Performance Optimization

- Index on (user_id, played_date) for daily log checks
- Index on (user_id) for reward queries
- Index on (month_year) for leaderboard queries
- Aggregation queries use database functions

## Future Enhancements

1. **Leaderboard UI:** Show top players of the month
2. **Reward History:** Display all won/used/expired rewards
3. **Multiplayer:** Head-to-head memory game mode
4. **Difficulty Settings:** Easy/Medium/Hard modes
5. **Daily Bonuses:** Higher rewards on consecutive play days
6. **Social Sharing:** Share score on social media
7. **Power-ups:** Extra time, hints, freeze tile
8. **Achievements:** Badges for milestones (100 plays, ₹500 earned, etc.)
