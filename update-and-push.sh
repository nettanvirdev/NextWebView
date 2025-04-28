#!/bin/bash

# First, let's fix the build.gradle file
cat > build.gradle << EOL
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id 'maven-publish' apply false
}
EOL

# Now let's add and commit all changes
git add build.gradle .github/workflows/android-ci.yml

# Commit the changes
git commit -m "Fix build.gradle and update actions/upload-artifact from v3 to v4"

# Push to master branch
git push origin master

# If you want to create or update a tag to trigger the release
git tag -d v1.0.0 2>/dev/null || true
git push --delete origin v1.0.0 2>/dev/null || true
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

echo "Changes pushed and CI/CD workflow triggered."
