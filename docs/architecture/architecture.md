# Architectural Pattern: Layered Architecture

Our team selected the Layered Architecture pattern for Subsystem 3 (Events and Resources).

## Why we chose this pattern

Layered architecture separates a system into horizontal layers, where each layer depends 
only on the layer below it. This keeps responsibilities cleanly divided: a change to how 
data is stored shouldn't ripple up into the calendar or resource-page logic, and a change 
to the calendar's behavior shouldn't require touching how data is stored. This separation 
makes the system easier to understand, test, and maintain, and it lets multiple team 
members work on different layers in parallel without stepping on each other.

## What each part does

**Calendar** — Handles the events/scheduling feature. Responsible for displaying, creating, 
and organizing calendar events for the user. This is one of the two feature modules users 
directly interact with.

**Resources** — Handles the resource landing page. Responsible for displaying and organizing 
links/information to career and event resources for students. The second of the two feature 
modules users directly interact with.

**Services** — Sits below both feature modules and contains the shared business logic they 
both rely on (logic that isn't specific to calendar events or specific to resources, 
but is used by both). Keeping this logic in one shared layer avoids duplicating the same 
code in both Calendar and Resources.

**Models** — Defines the data objects the system works with (e.g., an Event object, a 
Resource object) — the shape of the data, independent of how it's displayed or stored. 
Services reads and writes these objects rather than working with raw data directly.

**Storage** — The lowest layer, responsible for persisting and retrieving data (currently 
minimal/in progress). Only this layer knows the actual storage mechanism being used, so 
that detail can change later without affecting any layer above it.

## Why this fits our project

Subsystem 3 needs to provide two distinct user-facing features (a calendar and a resource 
landing page) that share underlying logic and data rather than duplicating it. Layering 
lets us build Calendar and Resources independently while keeping the logic and data they 
both depend on in one place.

## Deployment

The application is packaged with Docker so it builds and runs identically regardless of 
team members' local Java setup or IDE (VS Code, Eclipse, etc.).
