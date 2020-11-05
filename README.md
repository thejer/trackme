# TrackMe
TrackMe is an android application that communicates with a remote sever via TCP and uses the data from the server to show realtime locations of users.

> For this project, [MVVM Architecture](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel) and [Android Architecture Components](https://developer.android.com/jetpack/guide "Jetpack") was used

## Connection
In the ViewModel, a connection is opened to the server in a [Coroutine](https://developer.android.com/topic/libraries/architecture/coroutines) thread using Socket so data can be written to and read from data streams.
The data stream coming from the server in form of strings is parsed and then formatted into data objects that the app can use. Once any new data is read from the stream and parsed, they are posted to LiveData objects in the ViewModel.

## Map
[Google Maps SDK](https://developers.google.com/maps/documentation/android-sdk/overview) was used for this project. 
The Map View was inflated into the layout of the Launcher Activity of the app and in the onMapRead() function, 
I call the ViewModel function to open connection.

Leveraging the reactive nature of [LiveData](https://developer.android.com/topic/libraries/architecture/livedata), 
I observed the ViewModel's LiveData objects in the Activity's onCreate method, so once there is a new data from the server, the map view is updated accordingly.

 

## Demo
Here is a Demo of the app

[demo](demo.gif "Demo")