# Android Circle To Search

This app is based on Samsung and Google's Circle to Search feature. Because root app access level permissions are not available, a pop-up is shown to get screen recording permission each time.


<img src="/CircleToCrop.gif" width="300" height="668"/>


Steps-
- Get permission to show overlay on top
- Crop images added on top of view using WindowManager in a service
- Start recording using MediaProjection when the user taps on the Crop icon
- Added Paint view on top of view using WindowManager in another service
- Check for the user drawing a circle and capture frame boundaries
- Crop that section of the area and share it with Google Intent
- Make sure you select Google Lens as always for the share.
