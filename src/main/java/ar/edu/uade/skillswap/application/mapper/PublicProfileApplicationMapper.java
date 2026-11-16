package ar.edu.uade.skillswap.application.mapper;

import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.PublicProfileData.CompletedBookingsSummary;
import ar.edu.uade.skillswap.application.model.PublicProfileData.PublicProfileView;
import ar.edu.uade.skillswap.application.model.PublicProfileData.ReceivedRatingView;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.Rating;
import java.util.List;

/** Arma la vista pública eligiendo qué datos del perfil se publican. */
public final class PublicProfileApplicationMapper {
    private PublicProfileApplicationMapper() {}

    public static PublicProfileView toView(Profile profile, PageResult<Publication> publications,
                                           CompletedBookingsSummary completedBookings, List<Rating> recentRatings) {
        PageResult<PublicationView> activePublications =
                new PageResult<>(publications.items().stream()
                        .map(publication -> PublicationApplicationMapper.toView(publication, profile))
                        .toList(), publications.page(), publications.size(), publications.totalElements());
        return new PublicProfileView(profile.userId(), profile.actorType(), profile.displayName(),
                profile.biography(), List.copyOf(profile.skills()), profile.reputation(), profile.ratingCount(),
                activePublications, completedBookings,
                recentRatings.stream().map(PublicProfileApplicationMapper::toView).toList());
    }

    private static ReceivedRatingView toView(Rating rating) {
        return new ReceivedRatingView(rating.score(), rating.comment(), rating.createdAt());
    }
}
