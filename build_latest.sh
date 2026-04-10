#!/bin/bash
APP_VERSION="0.0.1"
APP_NAME="CodesScanner" #NO SPACES
APP_COMMENT="QR / Barcode scanner and generator"
APP_CATEGORY="Office;" # SEPARATE AND END USING SEMICOLON ';'
APP_EXEC="run"
ICON_FILE="codesscanner"
DESKTOP_FILE_NAME="codesscanner"
REPO_URL="gh-releases-zsync|brotenet|$APP_NAME|latest"
export ARCH=x86_64
export VERSION=$APP_VERSION
export UPDATE_INFO="$REPO_URL|$DESKTOP_FILE_NAME-$APP_VERSION-$ARCH.AppImage.zsync"

BASE_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
echo "BASE DIR: $BASE_DIR"

# Download AppImageTool
echo "Downloading latest AppImageTool..."
URL="https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage"
OUTPUT_FILE="appimagetool"
curl -L -o "$OUTPUT_FILE" "$URL"
chmod 777 $OUTPUT_FILE

# Create AppImage directories
echo "Creating AppImage directories..."
mkdir -p app_dir/usr

# Set AppRun execution rights
echo "Setting up AppRun permissions..."
chmod 777 AppRun

# Move application files 
echo "Moving application files..."
mv build app_dir/usr/bin
cp $ICON_FILE.png app_dir/
cp AppRun app_dir/

# Create .desktop file
echo "Creating .desktop file..."
DESKTOP_FILE="$DESKTOP_FILE_NAME.desktop"
DESKTOP_CONTENT=$(cat <<EOF
[Desktop Entry]
Name=$APP_NAME
Comment=$APP_COMMENT
Exec=$APP_EXEC
Icon=$ICON_FILE
Terminal=false
Type=Application
Categories=GNOME;GTK;$APP_CATEGORY
EOF
)

echo "$DESKTOP_CONTENT" > app_dir/"$DESKTOP_FILE"
chmod 644 app_dir/"$DESKTOP_FILE"

# Build AppImage package
echo "Building AppImage package..."
./appimagetool --updateinformation "$UPDATE_INFO" app_dir $DESKTOP_FILE_NAME-$APP_VERSION-$ARCH.AppImage

# Clean up of source files
echo "Cleaning up source files..."
rm -Rf app_dir
rm -f appimagetool

echo "AppImage packaging is complete!"
