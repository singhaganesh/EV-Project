package com.ganesh.EV_Project.config;

import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.enums.SlotType;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.repository.ChargerSlotRepository;
import com.ganesh.EV_Project.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

        @Autowired
        private StationRepository stationRepository;

        @Autowired
        private com.ganesh.EV_Project.repository.IoTSensorDataRepository ioTSensorDataRepository;

        @Autowired
        private ChargerSlotRepository chargerSlotRepository;

        @Override
        public void run(String... args) throws Exception {
                System.out.println("Data Seeding: proper seeding started...");

                List<Station> stationsToSeed = Arrays.asList(
                                Station.builder()
                                                .name("ChargePoint Mumbai Central")
                                                .address("Mumbai Central, Mumbai, Maharashtra 400008")
                                                .latitude(19.0760).longitude(72.8777)
                                                .meta("Fast charging available")
                                                .rating(4.7)
                                                .operatingHours("24 Hours").pricePerKwh(15.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("EcoCharge Bandra")
                                                .address("Bandra West, Mumbai, Maharashtra 400050")
                                                .latitude(19.0596).longitude(72.8295)
                                                .meta("Shopping mall parking")
                                                .rating(4.5)
                                                .operatingHours("6 AM - 11 PM").pricePerKwh(18.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Vashi EV Hub")
                                                .address("Sector 17, Vashi, Navi Mumbai, Maharashtra 400703")
                                                .latitude(19.0330).longitude(73.0297)
                                                .meta("24/7 Open")
                                                .rating(4.2)
                                                .operatingHours("24 Hours").pricePerKwh(14.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("GreenPower Thane")
                                                .address("Ghodbunder Road, Thane, Maharashtra 400607")
                                                .latitude(19.2183).longitude(72.9781)
                                                .meta("Highway access")
                                                .rating(4.0)
                                                .operatingHours("6 AM - 10 PM").pricePerKwh(16.0).isOpen(false)
                                                .build(),
                                Station.builder()
                                                .name("Powai Lake Chargers")
                                                .address("Powai, Mumbai, Maharashtra 400076")
                                                .latitude(19.1176).longitude(72.9060)
                                                .meta("Scenic view while charging")
                                                .rating(4.8)
                                                .operatingHours("7 AM - 11 PM").pricePerKwh(17.5).isOpen(true)
                                                .build(),
                                // Kolkata Stations
                                Station.builder()
                                                .name("EcoCharge Kolkata Center")
                                                .address("Park Street, Kolkata, West Bengal 700016")
                                                .latitude(22.5551).longitude(88.3514)
                                                .meta("Central business district")
                                                .rating(4.6)
                                                .operatingHours("24 Hours").pricePerKwh(14.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Salt Lake Sector V Hub")
                                                .address("Sector V, Salt Lake, Kolkata, West Bengal 700091")
                                                .latitude(22.5726).longitude(88.4374)
                                                .meta("IT Park area")
                                                .rating(4.4)
                                                .operatingHours("8 AM - 10 PM").pricePerKwh(15.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("New Town Green Energy")
                                                .address("New Town, Kolkata, West Bengal 700156")
                                                .latitude(22.5866).longitude(88.4616)
                                                .meta("Near Eco Park")
                                                .rating(4.3)
                                                .operatingHours("24 Hours").pricePerKwh(13.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("South City Mall Chargers")
                                                .address("Prince Anwar Shah Rd, Kolkata, West Bengal 700068")
                                                .latitude(22.5020).longitude(88.3615)
                                                .meta("Mall basement parking")
                                                .rating(4.7)
                                                .operatingHours("10 AM - 9 PM").pricePerKwh(19.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Gariahat EV Point")
                                                .address("Ballygunge Gardens, Gariahat, Kolkata, West Bengal 700019")
                                                .latitude(22.5180).longitude(88.3643)
                                                .meta("Market area parking")
                                                .rating(4.1)
                                                .operatingHours("7 AM - 10 PM").pricePerKwh(16.5).isOpen(false)
                                                .build(),
                                Station.builder()
                                                .name("Esplanade Metro Chargers")
                                                .address("Jawaharlal Nehru Road, Esplanade, Kolkata, West Bengal 700013")
                                                .latitude(22.5646).longitude(88.3517)
                                                .meta("Metro station access")
                                                .rating(4.5)
                                                .operatingHours("5 AM - 12 AM").pricePerKwh(15.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Science City Energy Hub")
                                                .address("J.B.S Haldane Avenue, Kolkata, West Bengal 700046")
                                                .latitude(22.5392).longitude(88.3963)
                                                .meta("Convention center parking")
                                                .rating(4.8)
                                                .operatingHours("24 Hours").pricePerKwh(12.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Dum Dum Airport Zone")
                                                .address("Jessore Road, Dum Dum, Kolkata, West Bengal 700028")
                                                .latitude(22.6420).longitude(88.4311)
                                                .meta("Airport approach road")
                                                .rating(4.2)
                                                .operatingHours("24 Hours").pricePerKwh(20.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Victoria Green Spot")
                                                .address("Queens Way, Maidan, Kolkata, West Bengal 700071")
                                                .latitude(22.5448).longitude(88.3426)
                                                .meta("Tourist spot parking")
                                                .rating(4.9)
                                                .operatingHours("6 AM - 9 PM").pricePerKwh(16.5).isOpen(true)
                                                .build(),
                                // Pune Stations (Batch 1 — 10 stations, 5-10 km apart)
                                Station.builder()
                                                .name("Hinjewadi IT Park Charger")
                                                .address("Phase 1, Hinjewadi, Pune, Maharashtra 411057")
                                                .latitude(18.5912).longitude(73.7380)
                                                .meta("IT Park campus, covered parking")
                                                .rating(4.6)
                                                .operatingHours("24 Hours").pricePerKwh(16.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Wakad Junction EV Point")
                                                .address("Datta Mandir Chowk, Wakad, Pune, Maharashtra 411057")
                                                .latitude(18.5990).longitude(73.7630)
                                                .meta("Near residential hub")
                                                .rating(4.3)
                                                .operatingHours("6 AM - 11 PM").pricePerKwh(15.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Baner Road Supercharger")
                                                .address("Baner Road, Baner, Pune, Maharashtra 411045")
                                                .latitude(18.5603).longitude(73.7860)
                                                .meta("Highway-adjacent, fast DC charging")
                                                .rating(4.8)
                                                .operatingHours("24 Hours").pricePerKwh(18.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Aundh Breman Chowk Charger")
                                                .address("Breman Chowk, Aundh, Pune, Maharashtra 411007")
                                                .latitude(18.5583).longitude(73.8083)
                                                .meta("University area parking")
                                                .rating(4.2)
                                                .operatingHours("7 AM - 10 PM").pricePerKwh(14.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Shivajinagar Metro Hub")
                                                .address("FC Road, Shivajinagar, Pune, Maharashtra 411004")
                                                .latitude(18.5314).longitude(73.8446)
                                                .meta("Metro station access")
                                                .rating(4.5)
                                                .operatingHours("5 AM - 12 AM").pricePerKwh(17.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Kothrud Depot EV Station")
                                                .address("Karve Nagar, Kothrud, Pune, Maharashtra 411038")
                                                .latitude(18.5070).longitude(73.8146)
                                                .meta("Bus depot integrated charging")
                                                .rating(4.0)
                                                .operatingHours("6 AM - 10 PM").pricePerKwh(13.5).isOpen(false)
                                                .build(),
                                Station.builder()
                                                .name("Swargate Interchange Charger")
                                                .address("Swargate, Pune, Maharashtra 411037")
                                                .latitude(18.5018).longitude(73.8636)
                                                .meta("Major transit hub")
                                                .rating(4.4)
                                                .operatingHours("24 Hours").pricePerKwh(15.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Koregaon Park Green Station")
                                                .address("Lane 6, Koregaon Park, Pune, Maharashtra 411001")
                                                .latitude(18.5363).longitude(73.8942)
                                                .meta("Premium area, valet assist")
                                                .rating(4.9)
                                                .operatingHours("8 AM - 11 PM").pricePerKwh(20.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Viman Nagar Airport Charger")
                                                .address("Dutta Nagar, Viman Nagar, Pune, Maharashtra 411014")
                                                .latitude(18.5679).longitude(73.9143)
                                                .meta("Airport approach, 24/7")
                                                .rating(4.7)
                                                .operatingHours("24 Hours").pricePerKwh(19.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Kharadi EON Hub Charger")
                                                .address("EON Free Zone, Kharadi, Pune, Maharashtra 411014")
                                                .latitude(18.5516).longitude(73.9405)
                                                .meta("IT SEZ parking, solar powered")
                                                .rating(4.6)
                                                .operatingHours("24 Hours").pricePerKwh(16.5).isOpen(true)
                                                .build(),
                                // Bengaluru Stations (Batch 2 — 10 stations, 5-10 km apart)
                                Station.builder()
                                                .name("Whitefield ITPL Charger")
                                                .address("ITPL Main Road, Whitefield, Bengaluru, Karnataka 560066")
                                                .latitude(12.9698).longitude(77.7500)
                                                .meta("IT Park campus, multi-level parking")
                                                .rating(4.5)
                                                .operatingHours("24 Hours").pricePerKwh(16.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Marathahalli Bridge EV Hub")
                                                .address("Marathahalli Bridge, Bengaluru, Karnataka 560037")
                                                .latitude(12.9591).longitude(77.7010)
                                                .meta("ORR junction access")
                                                .rating(4.3)
                                                .operatingHours("6 AM - 11 PM").pricePerKwh(15.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Indiranagar 100ft Road Charger")
                                                .address("100 Feet Road, Indiranagar, Bengaluru, Karnataka 560038")
                                                .latitude(12.9716).longitude(77.6412)
                                                .meta("Premium shopping district")
                                                .rating(4.7)
                                                .operatingHours("8 AM - 11 PM").pricePerKwh(19.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Koramangala Forum Charger")
                                                .address("Forum Mall, Koramangala, Bengaluru, Karnataka 560095")
                                                .latitude(12.9352).longitude(77.6245)
                                                .meta("Mall basement, valet service")
                                                .rating(4.8)
                                                .operatingHours("10 AM - 10 PM").pricePerKwh(20.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Electronic City Phase 1 Hub")
                                                .address("Infosys Gate, Electronic City, Bengaluru, Karnataka 560100")
                                                .latitude(12.8440).longitude(77.6602)
                                                .meta("Tech park zone, solar canopy")
                                                .rating(4.6)
                                                .operatingHours("24 Hours").pricePerKwh(14.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("JP Nagar 6th Phase Charger")
                                                .address("15th Cross, JP Nagar 6th Phase, Bengaluru, Karnataka 560078")
                                                .latitude(12.8987).longitude(77.5851)
                                                .meta("Residential area, overnight charging")
                                                .rating(4.1)
                                                .operatingHours("24 Hours").pricePerKwh(13.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Jayanagar 4th Block Station")
                                                .address("4th Block, Jayanagar, Bengaluru, Karnataka 560011")
                                                .latitude(12.9250).longitude(77.5838)
                                                .meta("Shopping complex parking")
                                                .rating(4.4)
                                                .operatingHours("7 AM - 10 PM").pricePerKwh(16.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Rajajinagar Metro EV Point")
                                                .address("Rajajinagar, Bengaluru, Karnataka 560010")
                                                .latitude(12.9916).longitude(77.5548)
                                                .meta("Metro station integrated")
                                                .rating(4.2)
                                                .operatingHours("5 AM - 12 AM").pricePerKwh(15.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Yeshwanthpur Industrial Charger")
                                                .address("Tumkur Road, Yeshwanthpur, Bengaluru, Karnataka 560022")
                                                .latitude(13.0227).longitude(77.5432)
                                                .meta("Industrial area, heavy vehicle support")
                                                .rating(4.0)
                                                .operatingHours("6 AM - 10 PM").pricePerKwh(12.5).isOpen(false)
                                                .build(),
                                Station.builder()
                                                .name("Hebbal Flyover Green Station")
                                                .address("Bellary Road, Hebbal, Bengaluru, Karnataka 560024")
                                                .latitude(13.0358).longitude(77.5970)
                                                .meta("Highway entry point, fast DC")
                                                .rating(4.7)
                                                .operatingHours("24 Hours").pricePerKwh(18.0).isOpen(true)
                                                .build(),
                                // Delhi NCR Stations (Batch 3 — 10 stations, 5-10 km apart)
                                Station.builder()
                                                .name("Connaught Place EV Hub")
                                                .address("Rajiv Chowk, Connaught Place, New Delhi, Delhi 110001")
                                                .latitude(28.6304).longitude(77.2177)
                                                .meta("Central market, public parking")
                                                .rating(4.6)
                                                .operatingHours("24 Hours").pricePerKwh(16.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Hauz Khas Village Charger")
                                                .address("Hauz Khas Village, Deer Park, New Delhi, Delhi 110016")
                                                .latitude(28.5494).longitude(77.2001)
                                                .meta("Tourist/Nightlife zone access")
                                                .rating(4.4)
                                                .operatingHours("8 AM - 12 AM").pricePerKwh(17.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Vasant Kunj Promenade Point")
                                                .address("Nelson Mandela Marg, Vasant Kunj, New Delhi, Delhi 110070")
                                                .latitude(28.5245).longitude(77.1555)
                                                .meta("Luxury mall parking")
                                                .rating(4.8)
                                                .operatingHours("10 AM - 11 PM").pricePerKwh(19.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Aerocity Premium Charge Area")
                                                .address("Hospitality District, Aerocity, New Delhi, Delhi 110037")
                                                .latitude(28.5491).longitude(77.1213)
                                                .meta("Airport hotel zone, 24/7 fast charge")
                                                .rating(4.9)
                                                .operatingHours("24 Hours").pricePerKwh(20.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Dwarka Sector 21 Metro EV")
                                                .address("Sector 21, Dwarka, New Delhi, Delhi 110077")
                                                .latitude(28.5523).longitude(77.0583)
                                                .meta("Metro interchange parking")
                                                .rating(4.3)
                                                .operatingHours("5 AM - 11 PM").pricePerKwh(15.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Cyber Hub Gurugram Node")
                                                .address("DLF Cyber City, DLF Phase 2, Gurugram, Haryana 122002")
                                                .latitude(28.4950).longitude(77.0895)
                                                .meta("Corporate park, rapid DC")
                                                .rating(4.7)
                                                .operatingHours("24 Hours").pricePerKwh(18.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Sector 50 Gurugram Reserve")
                                                .address("Sector 50, Gurugram, Haryana 122018")
                                                .latitude(28.4125).longitude(77.0655)
                                                .meta("Residential integrated charging")
                                                .rating(4.2)
                                                .operatingHours("6 AM - 10 PM").pricePerKwh(14.5).isOpen(false)
                                                .build(),
                                Station.builder()
                                                .name("Okhla Industrial Phase 1")
                                                .address("Okhla Industrial Estate, Phase 1, New Delhi, Delhi 110020")
                                                .latitude(28.5284).longitude(77.2797)
                                                .meta("Industrial park charging zone")
                                                .rating(4.1)
                                                .operatingHours("24 Hours").pricePerKwh(14.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Noida Sector 18 EV Point")
                                                .address("Sector 18 Market, Noida, Uttar Pradesh 201301")
                                                .latitude(28.5698).longitude(77.3235)
                                                .meta("Commercial hub parking")
                                                .rating(4.5)
                                                .operatingHours("8 AM - 11 PM").pricePerKwh(17.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Noida Electronic City Junction")
                                                .address("Sector 62, Noida, Uttar Pradesh 201309")
                                                .latitude(28.6186).longitude(77.3828)
                                                .meta("IT Park and Metro station")
                                                .rating(4.6)
                                                .operatingHours("24 Hours").pricePerKwh(16.0).isOpen(true)
                                                .build(),
                                // Hyderabad Stations (Batch 4 — 10 stations, 5-10 km apart)
                                Station.builder()
                                                .name("HITEC City Cyber Towers Charge")
                                                .address("Patrika Nagar, HITEC City, Hyderabad, Telangana 500081")
                                                .latitude(17.4435).longitude(78.3772)
                                                .meta("Tech park core, solar canopy")
                                                .rating(4.7)
                                                .operatingHours("24 Hours").pricePerKwh(16.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Gachibowli Stadium EV Point")
                                                .address("Old Mumbai Hwy, Gachibowli, Hyderabad, Telangana 500032")
                                                .latitude(17.4401).longitude(78.3489)
                                                .meta("Sports complex parking")
                                                .rating(4.5)
                                                .operatingHours("6 AM - 11 PM").pricePerKwh(15.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Jubilee Hills Checkpost Hub")
                                                .address("Jubilee Hills, Hyderabad, Telangana 500033")
                                                .latitude(17.4326).longitude(78.4071)
                                                .meta("Premium district, fast DC")
                                                .rating(4.8)
                                                .operatingHours("24 Hours").pricePerKwh(19.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Banjara Hills City Center")
                                                .address("Road No. 1, Banjara Hills, Hyderabad, Telangana 500034")
                                                .latitude(17.4156).longitude(78.4357)
                                                .meta("Mall basement parking")
                                                .rating(4.6)
                                                .operatingHours("10 AM - 11 PM").pricePerKwh(18.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Secunderabad Railway Charge")
                                                .address("Regimental Bazaar, Secunderabad, Telangana 500003")
                                                .latitude(17.4330).longitude(78.5042)
                                                .meta("Transit hub parking")
                                                .rating(4.3)
                                                .operatingHours("24 Hours").pricePerKwh(14.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Begumpet Airport Road")
                                                .address("Prakash Nagar, Begumpet, Hyderabad, Telangana 500016")
                                                .latitude(17.4447).longitude(78.4664)
                                                .meta("High traffic zone, quick charge")
                                                .rating(4.2)
                                                .operatingHours("6 AM - 10 PM").pricePerKwh(15.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Kukatpally Nexus Mall")
                                                .address("KPHB Phase 6, Kukatpally, Hyderabad, Telangana 500072")
                                                .latitude(17.4849).longitude(78.4024)
                                                .meta("Shopping mall parking")
                                                .rating(4.7)
                                                .operatingHours("10 AM - 10 PM").pricePerKwh(17.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Madhapur Inorbit Point")
                                                .address("Mindspace, Madhapur, Hyderabad, Telangana 500081")
                                                .latitude(17.4483).longitude(78.3915)
                                                .meta("Lake view parking")
                                                .rating(4.9)
                                                .operatingHours("8 AM - 12 AM").pricePerKwh(20.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Kondapur Botanical Garden")
                                                .address("Kondapur, Hyderabad, Telangana 500084")
                                                .latitude(17.4622).longitude(78.3587)
                                                .meta("Eco-friendly reserve area")
                                                .rating(4.4)
                                                .operatingHours("6 AM - 9 PM").pricePerKwh(14.0).isOpen(false)
                                                .build(),
                                Station.builder()
                                                .name("Mehdipatnam PVNR Express Hub")
                                                .address("Gudimalkapur, Mehdipatnam, Hyderabad, Telangana 500028")
                                                .latitude(17.3916).longitude(78.4400)
                                                .meta("Highway flyover access point")
                                                .rating(4.1)
                                                .operatingHours("24 Hours").pricePerKwh(13.5).isOpen(true)
                                                .build(),
                                // Chennai Stations (Batch 5 — 10 stations, 5-10 km apart)
                                Station.builder()
                                                .name("T Nagar Pondy Bazaar EV")
                                                .address("Pondy Bazaar, T Nagar, Chennai, Tamil Nadu 600017")
                                                .latitude(13.0418).longitude(80.2341)
                                                .meta("Shopping district parking")
                                                .rating(4.5)
                                                .operatingHours("8 AM - 10 PM").pricePerKwh(16.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Anna Nagar Tower Park Charger")
                                                .address("2nd Main Road, Anna Nagar, Chennai, Tamil Nadu 600040")
                                                .latitude(13.0850).longitude(80.2101)
                                                .meta("Premium residential area")
                                                .rating(4.7)
                                                .operatingHours("6 AM - 11 PM").pricePerKwh(14.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Adyar Depot Green Hub")
                                                .address("Lattice Bridge Road, Adyar, Chennai, Tamil Nadu 600020")
                                                .latitude(13.0033).longitude(80.2566)
                                                .meta("MTC depot integrated")
                                                .rating(4.2)
                                                .operatingHours("24 Hours").pricePerKwh(13.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Velachery Phoenix Mall Point")
                                                .address("Velachery Main Road, Chennai, Tamil Nadu 600042")
                                                .latitude(12.9808).longitude(80.2227)
                                                .meta("Mall basement, fast DC")
                                                .rating(4.8)
                                                .operatingHours("10 AM - 11 PM").pricePerKwh(19.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Guindy Industrial Charge Area")
                                                .address("Thiru Vi Ka Industrial Estate, Guindy, Chennai, Tamil Nadu 600032")
                                                .latitude(13.0064).longitude(80.2206)
                                                .meta("Tech park and industrial zone")
                                                .rating(4.4)
                                                .operatingHours("24 Hours").pricePerKwh(15.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("OMR Sholinganallur Hub")
                                                .address("OMR, Sholinganallur, Chennai, Tamil Nadu 600119")
                                                .latitude(12.8996).longitude(80.2279)
                                                .meta("IT Corridor priority station")
                                                .rating(4.9)
                                                .operatingHours("24 Hours").pricePerKwh(18.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Chromepet GST Road")
                                                .address("GST Road, Chromepet, Chennai, Tamil Nadu 600044")
                                                .latitude(12.9515).longitude(80.1408)
                                                .meta("Highway access point")
                                                .rating(4.3)
                                                .operatingHours("24 Hours").pricePerKwh(17.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Porur Junction EV Station")
                                                .address("Mount Poonamallee Road, Porur, Chennai, Tamil Nadu 600116")
                                                .latitude(13.0336).longitude(80.1585)
                                                .meta("Commercial junction")
                                                .rating(4.1)
                                                .operatingHours("6 AM - 10 PM").pricePerKwh(14.0).isOpen(false)
                                                .build(),
                                Station.builder()
                                                .name("Koyambedu Omni Bus Stand")
                                                .address("CMBT, Koyambedu, Chennai, Tamil Nadu 600107")
                                                .latitude(13.0694).longitude(80.1948)
                                                .meta("Major transit hub, rapid charge")
                                                .rating(4.6)
                                                .operatingHours("24 Hours").pricePerKwh(16.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Mylapore Kapaleeshwarar Area")
                                                .address("North Mada Street, Mylapore, Chennai, Tamil Nadu 600004")
                                                .latitude(13.0368).longitude(80.2676)
                                                .meta("Heritage site, compact parking")
                                                .rating(4.5)
                                                .operatingHours("5 AM - 9 PM").pricePerKwh(15.0).isOpen(true)
                                                .build(),
                                // Ahmedabad Stations (Batch 6 — 10 stations, 5-10 km apart)
                                Station.builder()
                                                .name("SG Highway Fast Point")
                                                .address("SG Highway, Bodakdev, Ahmedabad, Gujarat 380054")
                                                .latitude(23.0384).longitude(72.5119)
                                                .meta("Highway commercial zone")
                                                .rating(4.8)
                                                .operatingHours("24 Hours").pricePerKwh(16.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Bopal TRP Mall EV Point")
                                                .address("Bopal, Ahmedabad, Gujarat 380058")
                                                .latitude(23.0233).longitude(72.4645)
                                                .meta("Mall integrated parking")
                                                .rating(4.5)
                                                .operatingHours("10 AM - 11 PM").pricePerKwh(15.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Satellite Green Charge")
                                                .address("Ramdev Nagar, Satellite, Ahmedabad, Gujarat 380015")
                                                .latitude(23.0270).longitude(72.5208)
                                                .meta("Dense residential area")
                                                .rating(4.6)
                                                .operatingHours("6 AM - 10 PM").pricePerKwh(14.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Navrangpura University Area")
                                                .address("CG Road, Navrangpura, Ahmedabad, Gujarat 380009")
                                                .latitude(23.0360).longitude(72.5516)
                                                .meta("Student and shopping zone")
                                                .rating(4.3)
                                                .operatingHours("8 AM - 11 PM").pricePerKwh(16.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Ashram Road Riverfront")
                                                .address("Sabarmati Riverfront, Ashram Road, Ahmedabad, Gujarat 380009")
                                                .latitude(23.0401).longitude(72.5714)
                                                .meta("Scenic view charging")
                                                .rating(4.9)
                                                .operatingHours("24 Hours").pricePerKwh(17.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Maninagar Kankaria Hub")
                                                .address("Kankaria Lake Front, Maninagar, Ahmedabad, Gujarat 380008")
                                                .latitude(22.9984).longitude(72.5925)
                                                .meta("Tourist hub parking")
                                                .rating(4.4)
                                                .operatingHours("5 AM - 10 PM").pricePerKwh(15.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Chandkheda Motera Charger")
                                                .address("Motera Stadium Road, Chandkheda, Ahmedabad, Gujarat 382424")
                                                .latitude(23.1093).longitude(72.5830)
                                                .meta("Sports stadium area")
                                                .rating(4.7)
                                                .operatingHours("24 Hours").pricePerKwh(18.0).isOpen(false)
                                                .build(),
                                Station.builder()
                                                .name("Prahlad Nagar Corporate Road")
                                                .address("Corporate Road, Prahlad Nagar, Ahmedabad, Gujarat 380015")
                                                .latitude(23.0116).longitude(72.5057)
                                                .meta("Business district premium spot")
                                                .rating(4.8)
                                                .operatingHours("24 Hours").pricePerKwh(19.0).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Gota Highway Junction")
                                                .address("Vandematram City, Gota, Ahmedabad, Gujarat 382481")
                                                .latitude(23.0909).longitude(72.5366)
                                                .meta("Ring road access point")
                                                .rating(4.2)
                                                .operatingHours("24 Hours").pricePerKwh(14.5).isOpen(true)
                                                .build(),
                                Station.builder()
                                                .name("Naroda GIDC Energy Station")
                                                .address("GIDC Phase 2, Naroda, Ahmedabad, Gujarat 382330")
                                                .latitude(23.0673).longitude(72.6565)
                                                .meta("Industrial park fast DC")
                                                .rating(4.1)
                                                .operatingHours("6 AM - 8 PM").pricePerKwh(13.5).isOpen(true)
                                                .build());

                int stationIndex = 0;
                for (Station station : stationsToSeed) {
                        if (!stationRepository.existsByName(station.getName())) {
                                Station savedStation = stationRepository.save(station);
                                createSlotsForStation(savedStation, stationIndex);
                                generateSensorDataForStation(savedStation);
                                System.out.println("Seeded station: " + station.getName());
                        } else {
                                System.out.println("Station already exists: " + station.getName());
                        }
                        stationIndex++;
                }

                System.out.println("Data Seeding: Completed.");
        }

        private void createSlotsForStation(Station station, int stationIndex) {
                // Vary the slot configuration per station for realistic data
                switch (stationIndex % 5) {
                        case 0: // 3 DC fast chargers
                                chargerSlotRepository.saveAll(Arrays.asList(
                                                ChargerSlot.builder().station(station).slotLabel("A1")
                                                                .slotType(SlotType.DC).connectorType(ConnectorType.CCS2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(50.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("A2")
                                                                .slotType(SlotType.DC).connectorType(ConnectorType.CCS2)
                                                                .status(SlotStatus.CHARGING).powerKw(50.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B1")
                                                                .slotType(SlotType.DC)
                                                                .connectorType(ConnectorType.CHADEMO)
                                                                .status(SlotStatus.AVAILABLE).powerKw(50.0).build()));
                                break;
                        case 1: // 2 AC + 2 DC mix
                                chargerSlotRepository.saveAll(Arrays.asList(
                                                ChargerSlot.builder().station(station).slotLabel("A1")
                                                                .slotType(SlotType.DC).connectorType(ConnectorType.CCS2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(30.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("A2")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(22.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B1")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.OCCUPIED).powerKw(22.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B2")
                                                                .slotType(SlotType.DC)
                                                                .connectorType(ConnectorType.CHADEMO)
                                                                .status(SlotStatus.AVAILABLE).powerKw(50.0).build()));
                                break;
                        case 2: // 4 AC slow chargers
                                chargerSlotRepository.saveAll(Arrays.asList(
                                                ChargerSlot.builder().station(station).slotLabel("A1")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(7.4).build(),
                                                ChargerSlot.builder().station(station).slotLabel("A2")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(7.4).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B1")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.MAINTENANCE).powerKw(7.4).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B2")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(22.0).build()));
                                break;
                        case 3: // 2 Tesla superchargers + 1 CCS2
                                chargerSlotRepository.saveAll(Arrays.asList(
                                                ChargerSlot.builder().station(station).slotLabel("A1")
                                                                .slotType(SlotType.DC)
                                                                .connectorType(ConnectorType.TESLA)
                                                                .status(SlotStatus.AVAILABLE).powerKw(120.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("A2")
                                                                .slotType(SlotType.DC)
                                                                .connectorType(ConnectorType.TESLA)
                                                                .status(SlotStatus.CHARGING).powerKw(120.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B1")
                                                                .slotType(SlotType.DC).connectorType(ConnectorType.CCS2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(60.0).build()));
                                break;
                        case 4: // 5 varied connectors
                                chargerSlotRepository.saveAll(Arrays.asList(
                                                ChargerSlot.builder().station(station).slotLabel("A1")
                                                                .slotType(SlotType.DC).connectorType(ConnectorType.CCS2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(50.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("A2")
                                                                .slotType(SlotType.DC)
                                                                .connectorType(ConnectorType.CHADEMO)
                                                                .status(SlotStatus.AVAILABLE).powerKw(50.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B1")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.OCCUPIED).powerKw(22.0).build(),
                                                ChargerSlot.builder().station(station).slotLabel("B2")
                                                                .slotType(SlotType.AC)
                                                                .connectorType(ConnectorType.TYPE_2)
                                                                .status(SlotStatus.AVAILABLE).powerKw(7.4).build(),
                                                ChargerSlot.builder().station(station).slotLabel("C1")
                                                                .slotType(SlotType.DC).connectorType(ConnectorType.GB_T)
                                                                .status(SlotStatus.MAINTENANCE).powerKw(60.0).build()));
                                break;
                }
        }

        private void generateSensorDataForStation(Station station) {
                int minutesAgo = (int) (Math.random() * 120);
                java.time.LocalDateTime timestamp = java.time.LocalDateTime.now().minusMinutes(minutesAgo);

                com.ganesh.EV_Project.model.IoTSensorData data = com.ganesh.EV_Project.model.IoTSensorData.builder()
                                .station(station)
                                .voltage(230.0 + (Math.random() * 10))
                                .current(15.0 + (Math.random() * 15))
                                .power(3.5 + (Math.random() * 3.5))
                                .timestamp(timestamp)
                                .build();

                station.setLastUsedTime(timestamp);
                stationRepository.save(station);
                ioTSensorDataRepository.save(data);
        }
}
