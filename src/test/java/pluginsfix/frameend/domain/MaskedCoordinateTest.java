package pluginsfix.frameend.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskedCoordinateTest {

    @Test
    void shouldMaskPositiveNumbers() {
        String result = MaskedCoordinate.mask(1450, 2, '*');
        assertThat(result).isEqualTo("14**");
    }

    @Test
    void shouldMaskNegativeNumbers() {
        String result = MaskedCoordinate.mask(-3820, 2, '*');
        assertThat(result).isEqualTo("-38**");
    }

    @Test
    void shouldMaskSmallNumbersGracefully() {
        String result = MaskedCoordinate.mask(64, 2, '*');
        assertThat(result).isEqualTo("**");
    }

    @Test
    void shouldMaskSingleDigitNegative() {
        String result = MaskedCoordinate.mask(-5, 1, '*');
        assertThat(result).isEqualTo("-*");
    }
}
