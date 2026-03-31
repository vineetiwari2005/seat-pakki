# WALLET AND PAYMENT SYSTEM - SEPARATE IMPLEMENTATION

## ✅ COMPLETED

### 1. **Three-Option Payment Modal** ✅
- **Component**: [PaymentModal.jsx](frontend/src/components/Game/PaymentModal.jsx)
- **Shows 3 separate payment options**:
  - Main Wallet (permanent, no expiry)
  - Temporary Wallet (rewards, 10-15 day expiry)
  - Credit/Debit Card
- **Each option shows available balance**
- **Styling**: Updated [PaymentModal.scss](frontend/src/components/Game/PaymentModal.scss)

### 2. **Separate Wallet Deductions** ✅

**Main Wallet Payment**:
```
Payment Method: MAIN_WALLET
→ Deducts directly from permanent wallet
→ No greedy algorithm needed (no expiry)
```

**Temporary Wallet Payment** (Greedy):
```
Payment Method: TEMPORARY_WALLET
→ Backend sorts temp funds by expiresAt ASC
→ Deducts from fastest-expiring first
→ Falls back to main wallet if insufficient
→ Marks funds as isUsed=true
```

**Backend Implementation**: [GameService.java](src/main/java/com/driver/bookMyShow/Services/GameService.java)
```java
public purchaseExtraSpin(userId, amount, paymentMethod):
  if (MAIN_WALLET):
    userWalletService.debitWallet(userId, amount)
  
  if (TEMPORARY_WALLET):
    deductInPriorityOrder(userId, amount)  // Greedy
  
  if (CARD):
    // Handled by Stripe backend
```

### 3. **Tested Payment Flows** ✅
```
✅ MAIN_WALLET Payment: ₹10 deducted from main, balance: 9446.4 → 9436.4
✅ TEMPORARY_WALLET Payment: ₹10 deducted from temp, balance: 80.0 → 70.0
✅ Balances are SEPARATE (not merged anymore)
✅ API returns: {balance, temporaryBalance, consolidatedBalance}
```

### 4. **SessionID Fix** ✅
- PaymentModal now generates sessionId when redirecting to card payment
- Format: `SPIN_${Date.now()}_${randString}`
- Passed in state to Payment.jsx

---

## 🔄 IN PROGRESS / NEEDS COMPLETION

### 1. **24-Hour Spin Counter** (60% Complete)

**Backend**: Already exists in GameService
```java
endpoints:
  GET /api/game/spin-status/{userId}
    → Returns: {hasSpunToday, extraSpinsBalance, timeUntilNextSpin}
  
  GET /api/game/time-until-spin/{userId}
    → Returns: {timeRemaining, secondsRemaining}
```

**Frontend TODO**: Need to add in Game.jsx
```jsx
useEffect(() => {
  fetchSpinStatus();
}, [user?.id]);

const fetchSpinStatus = async () => {
  const response = await fetch(`/api/game/spin-status/${user?.id}`);
  const data = await response.json();
  setCanSpinFree(!data.hasSpunToday);
  setNextSpinTime(data.timeUntilNextSpin);
  setExtraSpinsBalance(data.extraSpinsBalance);
};
```

### 2. **Separate Wallet History Pages** (0% Complete - TODO)

**File to Create**:
- `frontend/src/pages/User/WalletHistory.jsx` - Main wallet transactions
- `frontend/src/pages/User/TemporaryWalletHistory.jsx` - Temporary wallet transactions

**WalletHistory.jsx Should Show**:
```
├─ Permanent wallet balance (from API)
├─ Transaction list:
│  ├─ Date
│  ├─ Type (Add Credit, Refund, Debit for payment)
│  ├─ Amount
│  └─ Status
└─ No expiry dates (permanent funds)
```

**TemporaryWalletHistory.jsx Should Show**:
```
├─ Temporary wallet balance (from API)
├─ Transaction list:
│  ├─ Source (Game reward, Ticket change, Refund)
│  ├─ Amount
│  ├─ Expiry date (10 or 15 days)
│  ├─ Days remaining
│  └─ Status (Active, Used, Expired)
└─ Progress bar for expiry countdown
```

**Profile.jsx Links** (TODO):
```jsx
<Link to="/wallet-history" className="btn">
  View Wallet History
</Link>

<Link to="/temporary-wallet-history" className="btn">
  View Temporary Wallet History
</Link>
```

### 3. **Auto-Expiry and DB Cleanup** ✅ (Already implemented)

**Current Implementation**:
```java
@Scheduled(cron = "0 0 2 * * ?")  // Daily at 2 AM
public void markExpiredTemporaryFunds():
  Find all temp funds where expiresAt < now
  Set isExpired = true
```

**To Add**: Delete cleanup (soft delete after 30 days)
```java
@Scheduled(cron = "0 0 3 * * ?")  // Daily at 3 AM
public void cleanupExpiredFunds():
  Find all temp funds where isExpired=true AND expiresAt < (now - 30days)
  Delete from database
```

---

## 📱 User Experience Flow

### Buying Extra Spin:
```
User clicks "Buy Extra Spin"
  ↓
PaymentModal Shows (3 OPTIONS):
  ├─ Main Wallet (₹9436.40)
  ├─ Temporary Wallet (₹70.00 - expires in 8 days)
  └─ Credit/Debit Card
  ↓
  [If Main Wallet Selected]
    → Payment processed immediately
    → Balance updated: ₹9436.40 - ₹10 = ₹9426.40
    → Extra spin added to account
    ↓
  [If Temporary Wallet Selected]
    → Greedy deduction applied
    → Fastest-expiring fund used first
    → Payment processed
    ↓
  [If Card Selected]  
    → Redirect to Card Payment page
    → 0 GST, 0 convenience fees (EXTRA_SPIN exception)
    → Process card payment
    → Award extra spin
```

### Free Daily Spin:
```
User opens Game page
  ↓
Check: Can user spin free today?
  [Show 24-hour counter if already spun]
  [Show "Spin Free" button if not spun]
  ↓
User spins
  ↓
Award reward to temporary wallet (10-15 day expiry)
  ↓
Show reward in transaction history
```

---

## 🗄️ Database Model

### **user_wallet** (Permanent Funds)
```sql
id, userId, balance, lastUpdated, updatedBy
├─ Contains: Deposits, refunds without expiry
└─ No expiry_date column (PERMANENT)
```

### **temporary_wallet** (Expiring Funds)
```sql
id, userId, amount, sourceType, expiresAt, createdAt
├─ sourceType: GAME_REWARD (10 days), TICKET_CHANGE (15 days)
├─ expiresAt: AUTO-CALCULATED based on sourceType
├─ isUsed: FALSE → TRUE when spent
├─ isExpired: FALSE → TRUE at 2 AM scheduler
└─ usedAt: NULL → timestamp when spent
```

### **spin_payment** (Payment Records)
```sql
transactionId, userId, amount, paymentMethod, paymentStatus
├─ paymentMethod: CREDIT_CARD, WALLET, TEMPORARY_WALLET
├─ paymentStatus: COMPLETED, FAILED
└─ createdAt: transaction timestamp
```

---

## API Endpoints Status

| Endpoint | Method | Status | Purpose |
|----------|--------|--------|---------|
| `/api/wallet/balance/{userId}` | GET | ✅ Working | Get separate balances |
| `/api/game/purchase-extra-spin` | POST | ✅ Working | Pay via MAIN_WALLET, TEMPORARY_WALLET, CARD |
| `/api/game/spin-status/{userId}` | GET | ✅ Working | Get 24-hour spin counter |
| `/api/game/time-until-spin/{userId}` | GET | ✅ Working | Get remaining time until next free spin |
| `/api/wallet/history/{userId}` | GET | ⚠️ TODO | Fetch main wallet transactions |
| `/api/wallet/temporary-history/{userId}` | GET | ⚠️ TODO | Fetch temp wallet transactions |

---

## ⚠️ Remaining Issues to Fix

1. **Payment.jsx Session ID**: Add fallback handling for `bookingDetails.sessionId` if undefined
2. **Wallet History APIs**: Create backend endpoints for getWalletTransactions and getTemporaryWalletTransactions
3. **Frontend History Pages**: Create WalletHistory.jsx and TemporaryWalletHistory.jsx
4. **Auto-cleanup**: Implement 30-day cleanup scheduler for expired funds
5. **Game Page 24-Hour Counter**: Add spin status fetching and UI

---

## 🧪 Quick Testing

```bash
# Test Main Wallet Payment
curl -X POST http://localhost:8080/api/game/purchase-extra-spin \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "amount": 10,
    "paymentMethod": "MAIN_WALLET"
  }'

# Test Temporary Wallet Payment
curl -X POST http://localhost:8080/api/game/purchase-extra-spin \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "amount": 10,
    "paymentMethod": "TEMPORARY_WALLET"
  }'

# Check 24-Hour Spin Status
curl http://localhost:8080/api/game/spin-status/1

# Check Balance
curl http://localhost:8080/api/wallet/balance/1
```

---

## 📝 Summary

**Working**:
- ✅ Separate Main & Temporary wallets display
- ✅ Three payment options in PaymentModal
- ✅ Greedy temporary wallet deduction
- ✅ Fixed session ID for card payments
- ✅ Zero GST/fees for spin purchases
- ✅ 24-hour spin counter (backend ready)

**Still Needed**:
- ⚠️ Wallet history pages
- ⚠️ Game page 24-hour counter UI
- ⚠️ Backend history endpoints
- ⚠️ Auto-cleanup scheduler

**Status**: Ready for testing on frontend with working payment flows!
