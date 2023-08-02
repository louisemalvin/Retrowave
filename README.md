# Retrowave - A Retro-Themed WAV Waveform Visualizer
 ___A simple waveform visualiser.___


## Quick Overview:
- [Design Concept](#design-concept)
- [User Interface](#user-interface)
- [Waveform](#waveform)
- [Architechture](#architecture)
- [Animations](#animations)
- [Additional Features](#additional-features)
- [Known Bugs](#known-bugs)
- [Credits](#credits)

## Design Concept
The UI design is mostly inspired by google recorder app and some old-cassete tape player. I used Figma to do a quick sketch of the app. 

        Design concept:
![Header concept](references/header_concept.png)
![Design concept](references/design_concept.png) 

## User Interface
Now to the actual application design. It ended up mostly very similar to the base design concept, with some little detail changes:

    Application Overview
![App](references/app.png)        

    Playback indicator: Lights up when audio is playing.
![Indicator_off](references/indicator.png)
![Indicator_on](references/indicator_on.png)

    Custom buttons: stop, play, change audio.
![Buttons](references/buttons.png)

## Waveform
The basic implementation of the waveform analyzer draws points as circles on an interval and it draws line that connects them to one another. 

Although this work on smaller files, it renders way too much points on larger files. It results on heavy computational time, and results a cramped up lines (since it's trying to fit all of the points into the width of the screen.) In simple terms, we could imagine this as we're trying to render 4k images on a full-hd screen. Couple of changes:

- Introducing _step_ and _noise filter_ to the loop. With step we skip n-step amount of index to reduce density of the points that needs to be drawn. Noise filter will skip low amplitude points since it'll be buried under higher amplitude samples.


```kotlin
    for (i in 1 until waveForm.size step stepCount)
```

- Other than that, I removed circle drawing since it does not fit to the current theme. However, instead of drawing a straight line from a to b, the app draws a curved line between 2 points (x,y), with the middle point between them as control point:
```kotlin
    val controlX1 = (prevX + x) / 2
    val controlY1 = (prevY + y) / 2
```
- Now, lets take a look at these 2 pictures:
![overdraw 1](references/overdraw_1.png)
![overdraw 2](references/overdraw_2.png)
When we use drawLine() to visualize the waveform, it does a lot of overdraw on low-amplitude points. To solve this, we could use drawPath() instead. It reduces the UI jank tremendously, especially when the user interact with the seekbar to change the audio current position.

## Architecture
Quick overview on the whole application layout:

        MainViewModel: Responsible for UI states, MediaPlayer states.
        Custom Views: Responsible for handling animations, UI looks and appearance
        MainActivity: MainViewModel LiveData observers, View event listeners

I restructured the app towards MVVM architectural pattern (or more like, VVM, since there's no real model implemented currently). I did it to solve a couple of problem:
- Preserve state of the UI on configuration changes (ex. rotating the device causes the playback to stop)
- It's way easier and cleaner to implement dynamic values that needs to be shown in the UI, by creating livedata on viewmodel and let the view observe them.
```kotlin
    // example of dynamic values
    private val _timestamp = MutableLiveData<Long>()
    // read-only form of LiveData
    val timestamp: LiveData<Long> get() = _timestamp

    /* we observe on View (MainActivity): */

    // observers
    mediaPlayerViewModel.timestamp.observe(this) { timestamp ->
        _binding.timestampTextView.text = getFormattedTime(timestamp)
    }
```
- At last, even after seperating the logic to viewmodel, the main activity is already populated with lots of functions for event listeners and observers. It would be really hard to navigate through pretty quickly without restructuring the whole app.

## Animations!
If we play an audio while muted, the app doesn't look really promising. To solve this, I've added a couple of visual feedbacks for the user to 'feel' the audio just by looking at the app. Try the app to find out more about it! (_Looking at you, three-colored buttons._)

Other than that, I also added some animations on how the waveform is drawn. Now it appears starting from left to the right, instead of all at once.

### Technical Details
Currently, the app uses 3 different custom views, WaveformSlideBar, LayeredButtons, PlaybackIndicator.

All of them extends an abstract class, CustomView which has 3 notable functions:

```kotlin
abstract render()
    Function to recalculate coordinates / sizes for onDraw method to draw on the UI. 
    This function is called whenever the View detects size changes.

getAnimator(valueToAnimate: float)
    Basically, just a pre-filled ObjectAnimator Constructor

abstract SetAnimator(animationValue: float)
    Add some manipulation to onDraw with animationValue, then invalidate
    animator.start() will call this
```

- For the waveform, setAnimator splits points to [last chunk index] :: [current animation value] and append them to a buffer Path() object, which will be drawn by onDraw later.

- For the buttons, the 3D effect comes from 2 rectangle with different height. setAnimator will recalculate position of the top rectangle (and icon) according to the current animation value.

- Unfortunately, playback Indicator doesn't have any animations :(


## Additional Features

Some minor details of the app:
- Seekbar to re-position current audio playback   

    `Implemented with the provided seekbar with custom thumb and transparent progress`

- Dynamic title, timestamp, and total duration of the current loaded audio

    `Data preserved in MainViewModel and observed by MainActivityaaaaaa`

- Flexible button press state

    `Button maximum height could be manipulated programatically depending on the usage`
- Button animation when user hold it and release. (ex: short press on load button to loop through asset files, long press to open external media)

    `Implemented with OnTouchListener instead of OnClickListener, with some custom view animations`

## Known Bugs
Bugs to fix:
- MediaPlayer.currentPosition() sometimes emits descending value while playing audio. This caused seekbar thumb moves backward when playing a very small audio duration. 

- Waveform does not render properly when user rapidly change the audio.

## Credits
EOF. You reached the end of the README. Thanks for reading!

This is a solution to the Paradox Cat coding challenge.

-Louise Malvin Tanaka :D