# TODO / Issues for Friday Assistant

1. Alarm is not setting the time precisely (may be off by a few minutes or not matching user intent exactly).
2. After a "not supported" command, speech-to-text (STT) does not restart automatically; must be restarted manually.
3. Clarify where notes, reminders, and other data are saved (add documentation or UI for saved items).
4. Play music ambiguity: clarify which app to use, or allow user to choose/prefer a music app.
5. Weather: currently only TTS/Toast; integrate with a real weather API for live data.
6. Maps navigation: currently only TTS/Toast; integrate with Google Maps or another navigation app for real navigation.
7. Web search: currently only TTS/Toast; implement actual web search and answer reading.
8. Current time and date: ensure TTS is accurate and formatted well.
9. Swipe left/right: works, but not always complete or reliable in all apps/contexts.
10. Send button: after clicking, message should be redirected to WhatsApp (not just generic send).
11. Overlay animation: not working as expected (not showing or not animating).
12. Accessibility: not redirecting to settings automatically; user must enable manually (should prompt and redirect reliably).

---

## Next Steps
- Address each issue above in code and/or documentation.
- Push this project to Git (see below for instructions). 