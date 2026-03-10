# Project Brief

## Goal

Build a SmartRacket Android app that connects to smart rackets over BLE, tracks training sessions, and provides guided coaching features around practice.

## Constraints

- Platform: Android app with Jetpack Compose, Room, Hilt, WorkManager, BLE foreground services
- Device: Smart racket / paddle peripherals connected over BLE
- Data: Training sessions, strokes, device pairing, health telemetry
- Current database behavior: Room version 1 with destructive migration fallback

## Stakeholders

- Player using the SmartRacket app
- Product/design owner defining training flow
- Engineering team implementing BLE, UI, and session features

## Definition of Done

- [ ] Multi-sport selection exists before training and supports device default + session override
- [ ] Warm-up page is shown before active training and can be skipped
- [ ] Recurring rest reminders trigger during long active sessions
- [ ] Relevant tests and verification steps pass

---
Created: 2026-03-10 | Last Updated: 2026-03-10
