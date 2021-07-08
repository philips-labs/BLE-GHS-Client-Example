# BLE Generic Health Sensor Client Example

*Note: It is assumed the reader is familiar with Bluetooth Low Energy GATT Servers and Android development*

**Key concepts**:

This codebase is used as a demonstrator of the BLE Generic Health Sensor client to receive, parse and display data from a peripheral using the GHS specification features. A project with a GHS peripheral simulator Android application has been made open source and is available [here](https://github.com/philips-labs/BLE-GHS-Server-Simulator) and will also be used for Bluetooth SIG Interoperability Testing of the GHS specification. As such it will be contiously modified and extended as the GHS specification evolves.

**Description**:  

An example implementation of an Android BLE client app that receives, parses and displays data from a peripheral using the proposed Generic Health Sensor standard that is easily modified. It is built on top of the [Blessed for Android](https://github.com/weliem/blessed-android) open source project which provides a library to simplify BLE communications. A layer to make handling BLE services/characteristics is built on top of Blessed and used to create a GHS service handler.

This BLE client application supports peripherals using the Generic Health Sensor (GHS) GATT service that is currently under development in the Bluetooth SIG. As it is an evolving specification it can also be used as a "playground" for various BLE properties and data representation.

This service in turn is based on the IEEE 11073-10206 specification that specifies an Abstract Content Model (ACOM) for personal health device data - covering any type of health observation that can be modeled using the IEEE 11073-10101 nomenclature system.

These standards provide a long-sought solution for interoperable personal health devices using Bluetooth Low Energy. When adopted, the GHS GATT service will create both a common BLE and data model, reducing the integration and development efforts of using personal health devices in a broad set of healthcare solutions.

This project implements an example Bluetooth cleint via an Android application that is capable of receiving health observations to connected clients. In the example temperature (a simple numeric value), heart rate (a simple numeric value), SpO2 (another simple numeric value) and a PPG (array of values) observations are supported. The supported types can be easily extended by the developer.

The client works in conjunction with a GHS peripheral that can connected to a GHS service and manage the types of observation data specified. An open source project with a [GHS peripheral simulator Android application](https://github.com/philips-labs/BLE-GHS-Server-Simulator) is available  and will also be used for Bluetooth SIG Interoperability Testing of the GHS specification. As such it will be contiously modified and extended as the GHS specification evolves.

**Technology stack**: 

* The project is written in Kotlin and implements a standalone Android application
* For BLE communications this application uses the [Blessed for Android](https://github.com/weliem/blessed-android) library that simplifies BLE usage under Android. More information about Blessed can be found at the Blessed project page.
* The BLE, GHS and data model classes are separated from the Android UX classes for reusability in other GHS server prototypes.

**Status**:

Alpha - work in progress, in parallel with the specificaion work done in IEEE and the Bluetooth SIG.

Latest updates: link to the [CHANGELOG](CHANGELOG.md).

## Project Usage

The project should build and run. Note, the current versions used and tested are:
* Android Studio 4.1.3
* Kotlin 1.4.31

The packages in the project are as follows:
* ```com.philips.btclient.extensions``` - BluetoothBytesParser, BluetoothGattCharacteristic, Byte, ByteArray and List extensions that are used in the project (and generally useful)
* ```com.philips.btclient``` - Base classes for BluetoothServer (responsible for overall Server behavior in collaboration with the BluetoothPeripheralManager) and BaseService (the base class for creating service handlers for Device Information, Current Time, and Generic Health Sensor)
* ```com.philips.btclient.acom``` - Classes that handle the GATT Current Time and Device Information Services.
* ```com.philips.btclient.fhir``` - Classes that handle and suppor the Generic Health Sensor Service, including data models and emitting sample observations.
* ```com.philips.btclient.ghs``` - Activity, Fragments and Adapter to support the UI.
* ```com.philips.btclient.util``` - Activity, Fragments and Adapter to support the UI.

## Example Usage

The Simulator UX consists of a main screen with 3 tabbed pages.

The "Device Info" tab contains properties for the Device Information Service (currently the device name and model number).

The "Observations" tab controls:
* Controlling the types of observations to be sent on each emission.
* Starting and stopping a continous data emitter (with the period defined in the ObservationEmitter class via a emitterPeriod property)
* Emitting a single shot observation(s). 

The "Experimenal" tab has various options being evaluated (but not proposed) for the data format of the emitted observations (note if these are selected any BLE central receiver would need to understand the matching resulting data format). The details of each option are self describing and are not discussed in this document. For those not familiar with the options or terminology it is recommended to not select any of these options.

The developer is free to modify and extend each of these page Fragments and the underlying classes to support other types of devices, observations or data representations.

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
