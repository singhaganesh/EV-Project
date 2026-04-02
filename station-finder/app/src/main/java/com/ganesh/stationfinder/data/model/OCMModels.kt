package com.ganesh.stationfinder.data.model

import com.google.gson.annotations.SerializedName

data class OCMStation(
    @SerializedName("ID") val id: Long,
    @SerializedName("UUID") val uuid: String?,
    @SerializedName("OperatorInfo") val operatorInfo: OperatorInfo?,
    @SerializedName("UsageType") val usageType: UsageType?,
    @SerializedName("AddressInfo") val addressInfo: AddressInfo,
    @SerializedName("Connections") val connections: List<Connection>?,
    @SerializedName("NumberOfPoints") val numberOfPoints: Int?,
    @SerializedName("GeneralComments") val comments: String?
)

data class OperatorInfo(
    @SerializedName("Title") val title: String?,
    @SerializedName("WebsiteURL") val website: String?
)

data class UsageType(
    @SerializedName("Title") val title: String?,
    @SerializedName("IsPayAtLocation") val isPayAtLocation: Boolean?
)

data class AddressInfo(
    @SerializedName("Title") val title: String,
    @SerializedName("AddressLine1") val addressLine1: String,
    @SerializedName("AddressLine2") val addressLine2: String?,
    @SerializedName("Town") val town: String?,
    @SerializedName("StateOrProvince") val state: String?,
    @SerializedName("Postcode") val postcode: String?,
    @SerializedName("CountryID") val countryId: Int?,
    @SerializedName("Latitude") val latitude: Double,
    @SerializedName("Longitude") val longitude: Double,
    @SerializedName("ContactTelephone1") val phone: String?
)

data class Connection(
    @SerializedName("ID") val id: Long,
    @SerializedName("ConnectionType") val type: ConnectionType?,
    @SerializedName("StatusType") val status: StatusType?,
    @SerializedName("Level") val level: ChargingLevel?,
    @SerializedName("PowerKW") val powerKw: Double?,
    @SerializedName("Quantity") val quantity: Int?
)

data class ConnectionType(
    @SerializedName("Title") val title: String?, // e.g. CCS (Type 2)
    @SerializedName("FormalName") val formalName: String?
)

data class StatusType(
    @SerializedName("Title") val title: String?, // e.g. Operational
    @SerializedName("IsOperational") val isOperational: Boolean?
)

data class ChargingLevel(
    @SerializedName("Title") val title: String?, // e.g. Level 2 : Medium (Fast)
    @SerializedName("Comments") val comments: String?
)
