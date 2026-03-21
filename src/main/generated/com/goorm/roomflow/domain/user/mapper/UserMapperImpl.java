package com.goorm.roomflow.domain.user.mapper;

import com.goorm.roomflow.domain.user.dto.UserTO;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.entity.UserRole;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-21T18:44:08+0900",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.4.jar, environment: Java 21.0.10 (Azul Systems, Inc.)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserTO toUserTO(User user) {
        if ( user == null ) {
            return null;
        }

        UserTO userTO = new UserTO();

        userTO.setUserId( user.getUserId() );
        userTO.setName( user.getName() );
        userTO.setEmail( user.getEmail() );
        userTO.setPassword( user.getPassword() );
        if ( user.getRole() != null ) {
            userTO.setRole( user.getRole().name() );
        }

        return userTO;
    }

    @Override
    public User toUser(UserTO userTO) {
        if ( userTO == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.name( userTO.getName() );
        user.email( userTO.getEmail() );
        user.password( userTO.getPassword() );
        if ( userTO.getRole() != null ) {
            user.role( Enum.valueOf( UserRole.class, userTO.getRole() ) );
        }

        return user.build();
    }
}
