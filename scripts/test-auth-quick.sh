#!/bin/bash

# Quick Authorization Test
# Uses the /register endpoint to create fresh test users

set -e

AUTH_URL="http://localhost:8081/api/auth"
ORDERS_URL="http://localhost:8081/api/orders"

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}=== Account Authorization Test ===${NC}\n"

# Register Trader 1
echo -e "${YELLOW}1. Registering trader1...${NC}"
RESPONSE=$(curl -s -X POST "$AUTH_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "trader1",
    "email": "trader1@test.com",
    "password": "Password123!",
    "fullName": "Trader One"
  }')

TOKEN1=$(echo "$RESPONSE" | jq -r '.token')
ACCOUNT1=$(echo "$TOKEN1" | cut -d. -f2 | base64 -d 2>/dev/null | jq -r '.accountId' || echo "N/A")
echo -e "${GREEN}✓ Registered trader1${NC}"
echo "  Account: $ACCOUNT1"
echo "  Token: ${TOKEN1:0:50}..."

# Register Trader 2
echo -e "\n${YELLOW}2. Registering trader2...${NC}"
RESPONSE=$(curl -s -X POST "$AUTH_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "trader2",
    "email": "trader2@test.com",
    "password": "Password123!",
    "fullName": "Trader Two"
  }')

TOKEN2=$(echo "$RESPONSE" | jq -r '.token')
ACCOUNT2=$(echo "$TOKEN2" | cut -d. -f2 | base64 -d 2>/dev/null | jq -r '.accountId' || echo "N/A")
echo -e "${GREEN}✓ Registered trader2${NC}"
echo "  Account: $ACCOUNT2"
echo "  Token: ${TOKEN2:0:50}..."

# Test 1: Trader1 accessing own account
echo -e "\n${YELLOW}3. Trader1 accessing their own account ($ACCOUNT1)...${NC}"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$ORDERS_URL/account/$ACCOUNT1" \
  -H "Authorization: Bearer $TOKEN1")
if [ "$STATUS" = "200" ]; then
  echo -e "${GREEN}✓ Success (HTTP $STATUS)${NC}"
else
  echo -e "${RED}✗ Failed (HTTP $STATUS)${NC}"
fi

# Test 2: Trader1 accessing trader2's account (should fail)
echo -e "\n${YELLOW}4. Trader1 accessing trader2's account ($ACCOUNT2) [SHOULD FAIL]...${NC}"
RESPONSE=$(curl -s -X GET "$ORDERS_URL/account/$ACCOUNT2" \
  -H "Authorization: Bearer $TOKEN1")
STATUS=$(echo "$RESPONSE" | jq -r '.errorCode // "NO_ERROR"' 2>/dev/null)

if [ "$STATUS" = "AUTH-403" ]; then
  echo -e "${GREEN}✓ Correctly denied with AUTH-403${NC}"
  echo "  Message: $(echo "$RESPONSE" | jq -r '.message')"
else
  echo -e "${RED}✗ Expected AUTH-403 but got: $STATUS${NC}"
  echo "  Response: $RESPONSE"
fi

# Test 3: Trader2 accessing their own account
echo -e "\n${YELLOW}5. Trader2 accessing their own account ($ACCOUNT2)...${NC}"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$ORDERS_URL/account/$ACCOUNT2" \
  -H "Authorization: Bearer $TOKEN2")
if [ "$STATUS" = "200" ]; then
  echo -e "${GREEN}✓ Success (HTTP $STATUS)${NC}"
else
  echo -e "${RED}✗ Failed (HTTP $STATUS)${NC}"
fi

# Test 4: Trader2 accessing trader1's account (should fail)
echo -e "\n${YELLOW}6. Trader2 accessing trader1's account ($ACCOUNT1) [SHOULD FAIL]...${NC}"
RESPONSE=$(curl -s -X GET "$ORDERS_URL/account/$ACCOUNT1" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESPONSE" | jq -r '.errorCode // "NO_ERROR"' 2>/dev/null)

if [ "$STATUS" = "AUTH-403" ]; then
  echo -e "${GREEN}✓ Correctly denied with AUTH-403${NC}"
  echo "  Message: $(echo "$RESPONSE" | jq -r '.message')"
else
  echo -e "${RED}✗ Expected AUTH-403 but got: $STATUS${NC}"
  echo "  Response: $RESPONSE"
fi

echo -e "\n${BLUE}=== Test Complete ===${NC}"
