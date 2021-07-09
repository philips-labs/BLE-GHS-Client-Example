# BLE Generic Health Sensor Client Example

*Note: It is assumed the reader is familiar with Bluetooth Low Energy GATT Servers and Android development*

**Key concepts**:

This codebase is used as a demonstrator of the BLE Generic Health Sensor client to receive, parse and display data from a peripheral using the GHS specification features. A project with a GHS peripheral simulator Android application has been made open source and is available [here](https://github.com/philips-labs/BLE-GHS-Server-Simulator) and will also be used for Bluetooth SIG Interoperability Testing of the GHS specification. As such it will be contiously modified and extended as the GHS specification evolves.

**Description**:  

An example implementation of an Android BLE client app that receives, parses and displays data from a peripheral using the proposed Generic Health Sensor standard that is easily modified. It is built on top of the [Blessed for Android](https://github.com/weliem/blessed-android) open source project which provides a library to simplify BLE communications. A layer to make handling BLE services/characteristics is built on top of Blessed and used to create a GHS service handler.

This BLE client application supports peripherals using the Generic Health Sensor (GHS) GATT service that is currently under development in the Bluetooth SIG. As it is an evolving specification it can also be used as a "playground" for various BLE properties and data representation.

The observations received from the peripheral are modeled using objects based on the IEEE 11073-10206 specification that specifies an Abstract Content Model (ACOM) for personal health device data - covering any type of health observation that can be modeled using the IEEE 11073-10101 nomenclature system.

These standards provide a long-sought solution for interoperable personal health devices using Bluetooth Low Energy. When adopted, the GHS GATT service will create both a common BLE and data model, reducing the integration and development efforts of using personal health devices in a broad set of healthcare solutions.

This project implements an example Bluetooth client via an Android application that is capable of receiving health observations to connected clients. In the example temperature (a simple numeric value), heart rate (a simple numeric value), SpO2 (another simple numeric value) and a PPG (array of values) observations are supported. The supported types can be easily extended by the developer.

The client works in conjunction with a GHS peripheral that can connected to a GHS service and manage the types of observation data specified. An open source project with a [GHS peripheral simulator Android application](https://github.com/philips-labs/BLE-GHS-Server-Simulator) is available  and will also be used for Bluetooth SIG Interoperability Testing of the GHS specification. As such it will be contiously modified and extended as the GHS specification evolves.

**Technology stack**: 

* The project is written in Kotlin and implements a standalone Android application
* For BLE communications this application uses the [Blessed for Android](https://github.com/weliem/blessed-android) library that simplifies BLE usage under Android. More information about Blessed can be found at the Blessed project page.
* The BLE, GHS service handling and data model classes are separated from the Android UX classes for reusability in other prototypes.

**Status**:

Alpha - work in progress, in parallel with the specificaion work done in IEEE and the Bluetooth SIG.

Latest updates: link to the [CHANGELOG](CHANGELOG.md).

## Project Usage

The project should build and run. Note, the current versions used and tested are:
* Android Studio 4.2.1
* Kotlin 1.5.20

The packages in the project are as follows:
* ```com.philips.bleclient.ui``` - UI package with Android activities, custom view to display a waveform byte array, observation logging support, a BluetoothHandler class to interface to the Blessed library and manage service handlers (based on the ServiceHandler class in the package), scanning, providing a interface to listen to peripheral state changes (discovered/connect/disconnect)
* ```com.philips.bleclient.services``` - A ServiceHandlerManager class to interface to the Blessed library and manage service handlers (based on the ServiceHandler class in the package), scanning, providing a interface to listen to peripheral state changes (discovered/connect/disconnect). More info in the section "Service Handlers"
* ```com.philips.bleclient.services.ghs``` - Service handler for GHS BLE services/characteristics and parsing, data model support to receives bytes, create observations and send them to listeners (in this example application the main activity)
* ```com.philips.bleclient.acom``` - Classes for the ACOM data model and constants for observations. Used by the GHS service handler classes.
* ```com.philips.bleclient.fhir``` - FhirUploader that handle and support uploading observations to FHIR using okhttp to POST observations, which can be transformed into FHIR JSON strings.
* ```com.philips.bleclient.extensions``` - Observation, BluetoothBytesParser, BluetoothGattCharacteristic, Byte, ByteArray and List extensions that are used in the project (and generally useful)

## Service Handlers Overview
As mentioned above, the open source [Blessed for Android](https://github.com/weliem/blessed-android) library simplfies using BLE in Android. To make writing specific BLE peripheral "drivers" easier some additional classes are added in this client example. They are in the [com.philips.bleclient.service](https://github.com/philips-internal/ghs-client-example/tree/main/app/src/main/java/com/philips/bleclient/service) package.

Text for ServiceHandlerManager class
Text ServiceHandler
Text for GHS ServiceHandler

## Client Usage

The Client UX consists of a main screen with a button to start/stop BLE scanning, a list that shows found Generic Health Sensor peripherals (when scanning), a list of connected GHS devices, fields to display latest received observations (temperature, heart rate, SpO2, PPG waveform), a button to show a screen with the received observation log history (and clear it). There is also a button to open a screen with a switch to turn sending obseravtions to a FHIR server on/off and another switch for the FHIR server either the public server at hapi.fhir.org, or a private server with the observation POST endpoint specified in the text edit field.

## Known issues

Given the early state of the Generic Health Sensor (GHS) GATT service within the Bluetooth SIG changes to the code to track the specifcations will be frequent and called out in the [CHANGELOG](CHANGELOG.md).

## Contributing

This project was open sourced primarily to allow for those interested in the Bluetooth General Health Sensor specification to experiment with simulations of devices. We don't anticipate contributions, however, if there is a useful extension, feature, fix or other improvement the project authors and maintainers are eager to merge those into the project code base. More information can be found [here](CONTRIBUTING.md).

## Contact / Getting help

For further questions on this project, contact:
* Abdul Nabi - lead author - abdul.nabi@philips.com
* Erik Moll - development of the Bluetooth SIG GHS and IEEE ACOM specifications - erik.moll@philips.com
* Martijn van Welie - lead author of the Blessed library - martijn.van.welie@philips.com

## License
This code is available under the [MIT license](LICENSE.md).
