package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Dtos.RequestDtos.TheaterEntryDto;
import com.driver.bookMyShow.Dtos.RequestDtos.TheaterSeatEntryDto;
import com.driver.bookMyShow.Enums.SeatType;
import com.driver.bookMyShow.Enums.UserRole;
import com.driver.bookMyShow.Exceptions.TheaterIsNotPresentOnThisAddress;
import com.driver.bookMyShow.Exceptions.TheaterIsPresentOnThatAddress;
import com.driver.bookMyShow.Models.City;
import com.driver.bookMyShow.Models.Theater;
import com.driver.bookMyShow.Models.TheaterSeat;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Repositories.CityRepository;
import com.driver.bookMyShow.Repositories.TheaterRepository;
import com.driver.bookMyShow.Repositories.UserRepository;
import com.driver.bookMyShow.Transformers.TheaterTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TheaterService {

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get all theaters (public - used by login page dropdown)
     */
    public List<Theater> getAllTheaters() {
        return theaterRepository.findAll();
    }

    public String addTheater(TheaterEntryDto theaterEntryDto) throws Exception {
        if(theaterRepository.findByAddress(theaterEntryDto.getAddress()) != null) {
            throw new TheaterIsPresentOnThatAddress();
        }
        
        Theater theater = TheaterTransformer.theaterDtoToTheater(theaterEntryDto);

        // Associate with city if cityId is provided
        if(theaterEntryDto.getCityId() != null) {
            City city = cityRepository.findById(theaterEntryDto.getCityId())
                    .orElseThrow(() -> new Exception("City not found with id: " + theaterEntryDto.getCityId()));
            theater.setCity(city);
        }

        // Assign admin user if provided
        if (theaterEntryDto.getAdminUserId() != null) {
            User adminUser = userRepository.findById(theaterEntryDto.getAdminUserId())
                    .orElseThrow(() -> new Exception("Admin user not found"));
            if (adminUser.getRole() != UserRole.THEATER_OWNER) {
                throw new Exception("User must have THEATER_OWNER role to be assigned as theatre admin");
            }
            theater.setAdmin(adminUser);
        }

        theaterRepository.save(theater);
        return "Theater has been saved Successfully";
    }

    public String addTheaterSeat(TheaterSeatEntryDto entryDto) throws TheaterIsNotPresentOnThisAddress{
        if(theaterRepository.findByAddress(entryDto.getAddress()) == null) {
            throw new TheaterIsNotPresentOnThisAddress();
        }
        Integer noOfSeatsInRow = entryDto.getNoOfSeatInRow();
        Integer noOfPremiumSeats = entryDto.getNoOfPremiumSeat();
        Integer noOfClassicSeat = entryDto.getNoOfClassicSeat();
        String address = entryDto.getAddress();

        Theater theater = theaterRepository.findByAddress(address);

        List<TheaterSeat> seatList = theater.getTheaterSeatList();

        int counter = 1;
        int fill = 0;
        char ch = 'A';

        for(int i = 1; i <= noOfClassicSeat; i++) {
            String seatNo = Integer.toString(counter)+ch;

            ch++;
            fill++;
            if(fill == noOfSeatsInRow) {
                fill = 0;
                counter++;
                ch = 'A';
            }

            TheaterSeat theaterSeat = new TheaterSeat();
            theaterSeat.setSeatNo(seatNo);
            theaterSeat.setSeatType(SeatType.CLASSIC);
            theaterSeat.setTheater(theater);
            seatList.add(theaterSeat);
        }

        for(int i = 1; i <= noOfPremiumSeats; i++) {
            String seatNo = Integer.toString(counter)+ch;

            ch++;
            fill++;
            if(fill == noOfSeatsInRow) {
                fill = 0;
                counter++;
                ch = 'A';
            }

            TheaterSeat theaterSeat = new TheaterSeat();
            theaterSeat.setSeatNo(seatNo);
            theaterSeat.setSeatType(SeatType.PREMIUM);
            theaterSeat.setTheater(theater);
            seatList.add(theaterSeat);
        }

        theaterRepository.save(theater);

        return "Theater Seats have been added successfully";
    }
}
