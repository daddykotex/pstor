# Release

1. Go to https://play.google.com/console/u/0/developers/8994535355573441116
1. Navigate to the PSTOR page
1. Click on the Production link in the sidebar
1. Click on "New Release"
1. On Android Studio, change the build variant to: `release` or use `./build-release.sh`
1. Update the version
1. Build > Clean
1. Build > Generate Signed Bundle (`pass -c IT/Android-PKS/store/keys/pstor` and `pass -c IT/Android-PKS/store`)
1. Upload the bundle on the release page and follow the instructions on the release page