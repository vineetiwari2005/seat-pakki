package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Dtos.RequestDtos.UpdateProfileDto;
import com.driver.bookMyShow.Dtos.RequestDtos.UserEntryDto;
import com.driver.bookMyShow.Dtos.ResponseDtos.TicketResponseDto;
import com.driver.bookMyShow.Exceptions.UserAlreadyExistsWithEmail;
import com.driver.bookMyShow.Exceptions.UserDoesNotExists;
import com.driver.bookMyShow.Models.Ticket;
import com.driver.bookMyShow.Models.User;
import com.driver.bookMyShow.Repositories.UserRepository;
import com.driver.bookMyShow.Transformers.TicketTransformer;
import com.driver.bookMyShow.Transformers.UserTransformer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    OtpService otpService;

    public String addUser(UserEntryDto userEntryDto) throws UserAlreadyExistsWithEmail{
        if(userRepository.findByEmailId(userEntryDto.getEmailId()) != null) {
            throw new UserAlreadyExistsWithEmail();
        }
        User user = UserTransformer.userDtoToUser(userEntryDto);

        userRepository.save(user);
        return "User Saved Successfully";
    }

    public List<TicketResponseDto> allTickets(Integer userId) throws UserDoesNotExists{
        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()) {
            throw new UserDoesNotExists();
        }
        User user = userOpt.get();
        
        // Fetch ticket list from database (via lazy loading or join fetch)
        List<Ticket> ticketList = user.getTicketList();
        log.info("[FETCH_BOOKINGS] Fetched {} tickets from DB for userId: {}", ticketList.size(), userId);
        
        List<TicketResponseDto> ticketResponseDtos = new ArrayList<>();
        for(Ticket ticket : ticketList) {
            log.debug("[BOOKING] Ticket ID={}, bookedAt={}, seats={}", 
                ticket.getId(), ticket.getBookedAt(), ticket.getBookedSeats());
            TicketResponseDto ticketResponseDto = TicketTransformer.returnTicket(ticket.getShow(), ticket);
            ticketResponseDtos.add(ticketResponseDto);
        }
        return ticketResponseDtos;
    }

    public User getUserById(Integer userId) throws UserDoesNotExists {
        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()) {
            throw new UserDoesNotExists();
        }
        return userOpt.get();
    }

    /**
     * Update user profile (name and email)
     * 
     * @param userId User ID
     * @param updateDto Update profile DTO
     * @return Updated user
     * @throws UserDoesNotExists if user not found
     * @throws UserAlreadyExistsWithEmail if email already taken by another user
     */
    @Transactional
    public User updateProfile(Integer userId, UpdateProfileDto updateDto) throws UserDoesNotExists, UserAlreadyExistsWithEmail {
        // Check if user exists
        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()) {
            throw new UserDoesNotExists();
        }
        
        User user = userOpt.get();

        String existingEmail = user.getEmailId() == null ? "" : user.getEmailId().trim();
        String existingMobile = user.getMobileNo() == null ? "" : user.getMobileNo().trim();
        String requestedEmail = updateDto.getEmail() == null ? "" : updateDto.getEmail().trim();
        String requestedMobile = updateDto.getMobileNo() == null ? "" : updateDto.getMobileNo().trim();

        boolean isEmailChanged = !existingEmail.equalsIgnoreCase(requestedEmail);
        boolean isMobileChanged = !existingMobile.equals(requestedMobile);

        if (isEmailChanged || isMobileChanged) {
            try {
                otpService.verifyOtp(
                        userId,
                        updateDto.getOtpRequestId(),
                        updateDto.getOtpCode(),
                        "PROFILE_UPDATE",
                        String.valueOf(userId)
                );
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
        
        // Check if email is being changed and if new email is already taken by another user
        if (isEmailChanged) {
            User existingUserWithEmail = userRepository.findByEmailId(requestedEmail);
            if (existingUserWithEmail != null && !existingUserWithEmail.getId().equals(userId)) {
                throw new UserAlreadyExistsWithEmail();
            }
            user.setEmailId(requestedEmail);
        }

        if (isMobileChanged) {
            user.setMobileNo(requestedMobile);
        }
        
        // Update name
        user.setName(updateDto.getName());
        
        return userRepository.save(user);
    }
}
