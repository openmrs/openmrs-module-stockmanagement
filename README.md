Stock Management Module
==========================


## Stock locations

The module finds its stock-holding locations through location tags. On startup it makes sure the
`Main Store`, `Main Pharmacy` and `Dispensary` tags exist, but it does not create locations or tag
existing ones. Each implementation decides which of its own locations carry these tags, for example
with an [Initializer](https://github.com/mekomsolutions/openmrs-module-initializer) `locations`
file or from the location administration pages.
