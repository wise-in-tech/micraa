#!/bin/bash

# Script de test pour l'API Micraa
# Ce script crée des utilisateurs de test et une classe en direct

echo "======================================"
echo "Micraa API Test Script"
echo "======================================"
echo ""

BASE_URL="http://localhost:8080/api"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_step() {
    echo -e "${BLUE}>>> $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check if API is up
print_step "Checking API health..."
HEALTH=$(curl -s "$BASE_URL/health")
if [[ $? -eq 0 ]]; then
    print_success "API is healthy"
    echo "$HEALTH" | jq .
else
    print_error "API is not responding"
    exit 1
fi
echo ""

# Create Teacher
print_step "Creating teacher..."
TEACHER=$(curl -s -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Prof. Martin",
    "email": "martin@school.com",
    "role": "TEACHER"
  }')
TEACHER_ID=$(echo "$TEACHER" | jq -r '.id')
print_success "Teacher created with ID: $TEACHER_ID"
echo "$TEACHER" | jq .
echo ""

# Create Student 1
print_step "Creating student 1..."
STUDENT1=$(curl -s -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Dupont",
    "email": "alice@school.com",
    "role": "STUDENT"
  }')
STUDENT1_ID=$(echo "$STUDENT1" | jq -r '.id')
print_success "Student 1 created with ID: $STUDENT1_ID"
echo "$STUDENT1" | jq .
echo ""

# Create Student 2
print_step "Creating student 2..."
STUDENT2=$(curl -s -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bob Martin",
    "email": "bob@school.com",
    "role": "STUDENT"
  }')
STUDENT2_ID=$(echo "$STUDENT2" | jq -r '.id')
print_success "Student 2 created with ID: $STUDENT2_ID"
echo "$STUDENT2" | jq .
echo ""

# Create Live Class
print_step "Creating live class..."
LIVE_CLASS=$(curl -s -X POST "$BASE_URL/live-classes" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Mathématiques - Algèbre\",
    \"teacherId\": $TEACHER_ID,
    \"scheduledAt\": \"2026-08-21T10:00:00\"
  }")
CLASS_ID=$(echo "$LIVE_CLASS" | jq -r '.id')
print_success "Live class created with ID: $CLASS_ID"
echo "$LIVE_CLASS" | jq .
echo ""

# Add students to class
print_step "Adding students to live class..."
ADD_STUDENTS=$(curl -s -X POST "$BASE_URL/live-classes/$CLASS_ID/students" \
  -H "Content-Type: application/json" \
  -d "{
    \"studentIds\": [$STUDENT1_ID, $STUDENT2_ID]
  }")
print_success "Students added to class"
echo ""

# Start the class
print_step "Starting the live class..."
START_CLASS=$(curl -s -X POST "$BASE_URL/live-classes/$CLASS_ID/start?teacherId=$TEACHER_ID")
print_success "Live class started"
echo "$START_CLASS" | jq .
echo ""

# Teacher joins
print_step "Teacher joining the class..."
TEACHER_JOIN=$(curl -s -X POST "$BASE_URL/live-classes/$CLASS_ID/join?userId=$TEACHER_ID")
print_success "Teacher joined - LiveKit token generated"
echo "$TEACHER_JOIN" | jq .
echo ""

# Student 1 joins
print_step "Student 1 joining the class..."
STUDENT1_JOIN=$(curl -s -X POST "$BASE_URL/live-classes/$CLASS_ID/join?userId=$STUDENT1_ID")
print_success "Student 1 joined - LiveKit token generated"
echo "$STUDENT1_JOIN" | jq .
echo ""

# Student 2 joins
print_step "Student 2 joining the class..."
STUDENT2_JOIN=$(curl -s -X POST "$BASE_URL/live-classes/$CLASS_ID/join?userId=$STUDENT2_ID")
print_success "Student 2 joined - LiveKit token generated"
echo "$STUDENT2_JOIN" | jq .
echo ""

# Get class details
print_step "Getting live class details..."
CLASS_DETAILS=$(curl -s "$BASE_URL/live-classes/$CLASS_ID")
print_success "Class details retrieved"
echo "$CLASS_DETAILS" | jq .
echo ""

# List teacher's classes
print_step "Listing teacher's classes..."
TEACHER_CLASSES=$(curl -s "$BASE_URL/live-classes?userId=$TEACHER_ID&role=TEACHER")
print_success "Teacher's classes retrieved"
echo "$TEACHER_CLASSES" | jq .
echo ""

# List student's classes
print_step "Listing student's classes..."
STUDENT_CLASSES=$(curl -s "$BASE_URL/live-classes?userId=$STUDENT1_ID&role=STUDENT")
print_success "Student's classes retrieved"
echo "$STUDENT_CLASSES" | jq .
echo ""

echo "======================================"
echo -e "${GREEN}Test completed successfully!${NC}"
echo "======================================"
echo ""
echo "Summary:"
echo "  Teacher ID: $TEACHER_ID"
echo "  Student 1 ID: $STUDENT1_ID"
echo "  Student 2 ID: $STUDENT2_ID"
echo "  Live Class ID: $CLASS_ID"
echo ""
echo "Next steps:"
echo "  1. Use the LiveKit tokens to test audio in a client application"
echo "  2. To end the class, run:"
echo "     curl -X POST \"$BASE_URL/live-classes/$CLASS_ID/end?teacherId=$TEACHER_ID\""
