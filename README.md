# Release

1. Go to https://play.google.com/console/u/0/developers/8994535355573441116
2. Navigate to the PSTOR page, and click on "New Release"
3. On Android Studio, change the build variant to: `release`
4. Update the version
5. Build > Clean
6. Build > Generate Signed Bundle (`pass -c IT/Android-PKS/store/keys/pstor` and `pass -c IT/Android-PKS/store`)
7. Upload the bundle on the release page and follow the instructions on the release page