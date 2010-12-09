//Copyright (c) 2009, Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.



package com.soundcloud.android.utils.flip3d;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class Flip3dAnimation  extends Animation {
private final float mFromDegrees;
private final float mToDegrees;
private final float mCenterX;
private final float mCenterY;
private Camera mCamera;

public Flip3dAnimation(float fromDegrees, float toDegrees,
   float centerX, float centerY) {
mFromDegrees = fromDegrees;
mToDegrees = toDegrees;
mCenterX = centerX;
mCenterY = centerY;
}

@Override
public void initialize(int width, int height, int parentWidth, int parentHeight) {
super.initialize(width, height, parentWidth, parentHeight);
mCamera = new Camera();
}

@Override
protected void applyTransformation(float interpolatedTime, Transformation t) {
final float fromDegrees = mFromDegrees;
float degrees = fromDegrees + ((mToDegrees - fromDegrees) * interpolatedTime);

final float centerX = mCenterX;
final float centerY = mCenterY;
final Camera camera = mCamera;

final Matrix matrix = t.getMatrix();

camera.save();

camera.rotateY(degrees);

camera.getMatrix(matrix);
camera.restore();

matrix.preTranslate(-centerX, -centerY);
matrix.postTranslate(centerX, centerY);

}

}