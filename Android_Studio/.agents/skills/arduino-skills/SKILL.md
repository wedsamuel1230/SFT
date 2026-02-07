---
name: arduino-skills
description: Comprehensive collection of Arduino-related skills for embedded development, covering project building, code generation, BOM creation, battery selection, code review, datasheet interpretation, enclosure design, error explanation, power calculation, README generation, microcontroller expertise, PlatformIO development, FreeRTOS patterns, diagram generation, and circuit debugging.
category: embedded
---

# Arduino Skills Collection

This skill aggregates all Arduino-related skills available in the system, providing a comprehensive toolkit for Arduino/ESP32/RP2040 embedded development projects.

## Available Arduino Skills

### arduino-project-builder
Build complete, production-ready Arduino projects (environmental monitors, robot controllers, IoT devices, automation systems). Assembles multi-component systems combining sensors, actuators, communication protocols, state machines, data logging, and power management. Supports Arduino UNO, ESP32, and Raspberry Pi Pico with board-specific optimizations. Use this skill when users request complete Arduino applications, not just code snippets.

### arduino-code-generator
Generate Arduino/embedded C++ code snippets and patterns on demand for UNO/ESP32/RP2040. Use when users request Arduino code for sensors, actuators, communication protocols, state machines, non-blocking timers, data logging, or hardware abstraction. Generates production-ready code with proper memory management, timing patterns, and board-specific optimization. Supports DHT22, BME280, buttons, I2C/SPI, EEPROM, SD cards, WiFi, and common peripherals.

### bom-generator
Generates Bill of Materials (BOM) from project descriptions for Arduino/ESP32/RP2040 projects. Use when user needs component lists, parts shopping lists, cost estimates, or asks "what parts do I need". Outputs formatted BOMs with part numbers, quantities, suppliers (DigiKey, Mouser, Amazon, AliExpress), and compatibility warnings. Run scripts/generate_bom.py for xlsx/csv export.

### battery-selector
Helps choose the right battery type and charging solution for Arduino/ESP32/RP2040 projects. Use when user asks about battery options, charging circuits, power source selection, or says "what battery should I use". Covers chemistry selection, safety, voltage regulation, and charging circuits.

### code-review-facilitator
Automated code review for Arduino/ESP32/RP2040 projects focusing on best practices, memory safety, and common pitfalls. Use when user wants code feedback, says "review my code", needs help improving code quality, or before finalizing a project. Generates actionable checklists and specific improvement suggestions.

### datasheet-interpreter
Extracts key specifications from component datasheet PDFs for maker projects. Use when user shares a datasheet PDF URL, asks about component specs, needs pin assignments, I2C addresses, timing requirements, or register maps. Downloads and parses PDF to extract essentials. Complements datasheet-parser for quick lookups.

### enclosure-designer
Guides design and generation of 3D-printable enclosures for Arduino/ESP32/RP2040 projects. Use when user needs a case, box, housing, or enclosure for their electronics project. Provides parametric design guidance, OpenSCAD templates, STL generation tips, and print settings.

### error-message-explainer
Interprets Arduino/ESP32/RP2040 compiler errors in plain English for beginners. Use when user shares error messages, compilation failures, upload problems, or asks "what does this error mean". Covers common errors like undefined references, type mismatches, missing libraries, and board-specific issues.

### power-budget-calculator
Calculates total power consumption and battery life for Arduino/ESP32/RP2040 projects. Use when user asks about battery life, power requirements, current draw, or needs to estimate runtime. Includes sleep mode analysis, power optimization tips, and battery sizing recommendations. Run scripts/calculate_power.py for accurate calculations.

### readme-generator
Auto-generates professional README.md files for Arduino/ESP32/RP2040 projects following open-source best practices. Use when user wants to document their project for GitHub, needs help writing a README, or says "make my project shareable". Follows awesome-readme standards with sections for Overview, Hardware, Software, Setup, Usage, Troubleshooting, and Contributing.

### microcontroller expert
Autonomous Arduino IDE expert for AVR, ESP32, ESP8266, RP2040, STM32 boards. Writes clean sketches, debugs peripherals, manages libraries, and ships working firmware via the Arduino ecosystem. Uses MCP servers for research and documentation.

### platform-io
Autonomous PlatformIO Firmware Architect for ESP32, RP2040, RP2350, and STM32. Enforces professional R&D standards: C++ OOP, FreeRTOS, Unit Testing, and CI/CD pipelines.

### freertos-patterns
(Category: arduino) - Provides FreeRTOS patterns and examples for ESP32/RP2040 multicore development, including task management, synchronization, and Arduino-compatible APIs.

### mermaid-diagram-generator
Generates Mermaid diagrams from Arduino code to visualize state machines, timing, architecture, and workflows. Use when users need to visualize Arduino code structure, state machines, timing diagrams, or workflows.

### circuit-debugger
Systematic hardware debugging guide for Arduino/ESP32/RP2040 circuits. Use when user reports: circuit not working, components getting hot, no power, intermittent failures, unexpected behavior, sensor not responding, LED not lighting, motor not spinning. Guides through power checks, continuity testing, signal tracing, and component isolation using multimeter techniques.