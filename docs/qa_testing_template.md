Quality Assurance Testing Template
=====================

QA testing is done by testing functionality of each of the app's Fragments.
Since testing is currently done manually, test cases should be brief and focus on core functionality operating as expected under normal conditions (ie. good network and GPS connection).

### pre-QA testing tasks:
- [ ] Update dependencies
- [ ] fix all warnings and errors produced by the linter
- [ ] check TODOs

On a fresh app install test the following:

## CreateTripFragment

- [ ] create Trip
- [ ] create new gig label
- [ ] toggle themes (check for visual inconsistencies)

## EditTripFragment

- [ ] add/remove stop
- [ ] update payout
- [ ] update gig
- [ ] toggle themes

## TripsFragment

- [ ] verify recyclerview is reflecting correct number of Trips
- [ ] find Trip by searching
- [ ] tap pay total to show Earnings Dialog
- [ ] export Trips
- [ ] clear all Trips
- [ ] add Trips shortcut to home screen
- [ ] access Settings
- [ ] toggle themes

## CreateOrEditApartmentMapFragment

- [ ] create Map
- [ ] edit name of Map
- [ ] toggle themes

## EditAptBuildingsMapsFragment

- [ ] add marker
- [ ] remove marker
- [ ] navigate to marker
- [ ] change Map Type
- [ ] toggle themes

## MapsFragment

- [ ] verify recyclerview is reflecting correct number of Maps
- [ ] find Map by searching
- [ ] order by nearest
- [ ] add Maps shortcut to home screen
- [ ] show buildings
- [ ] navigate to building
- [ ] disassociate with gatecode
- [ ] delete Map
- [ ] toggle themes

## CreateGateCodeFragment

- [ ] create Gatecode
- [ ] toggle themes

## EditGateCodeFragment

- [ ] update gatecode properties
- [ ] add/remove gate code entries
- [ ] associate with a Map
- [ ] delete gatecode
- [ ] toggle themes

## GateCodesFragment

- [ ] find gatecode by searching
- [ ]  order by nearest
- [ ] toggle themes

## CreateCustomerFragment

- [ ] create Customer
- [ ] toggle themes

## EditCustomerFragment

- [ ] update customer properties
- [ ] delete customer
- [ ] toggle themes

## CustomersFragment

- [ ] find Customer by searching
- [ ] toggle themes

## GigLabelsFragment

- [ ] create gig label
- [ ] edit gig label
- [ ] hide gig label
- [ ] delete gig label
- [ ] rearrange gig label
- [ ] toggle themes


## CurrentStatusBubbleFragment

- [ ] create Trip
- [ ] add stop
- [ ] iterate through Trips
- [ ] navigate to building
- [ ] open map
- [ ] iterate through Maps
- [ ] toggle themes


## SettingsFragment

- [ ] toggle between Current Status Service states:
    - [ ] disabled
    - [ ] notification group (test functionality also)
    - [ ] conversation Bubble
- [ ] create/restore backup
- [ ] enable auto backup
- [ ] In About section, info is accurate
- [ ] In About section, tapping changelog goes to github repo commits page
- [ ] developer options should be permanently enabled in debug build
- [ ] developer options should only be enabled after User action (tap Build 8 times) in release build
- [ ] toggle themes

### post-QA testing tasks:
- [ ] Update README.md (check license year, banner image, examples, etc)
- [ ] update docs, [example](https://github.com/square/okhttp/tree/master/docs)

