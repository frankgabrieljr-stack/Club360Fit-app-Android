import SwiftUI

/// Club360Fit design system — **Burgundy & Cream** palette (`#800020`, `#F9F9DC`, `#8A7E72`).
/// Legacy names (`teal`, `mint`, `purple`, …) are kept as aliases so existing UI compiles unchanged.
enum Club360Theme {
    // MARK: Brand (canonical hex)

    /// `#800020` — primary brand, CTAs, nav emphasis, icons.
    static let burgundy = Color(red: 128 / 255, green: 0 / 255, blue: 32 / 255)

    /// `#F9F9DC` — surfaces, glass bases, screen wash.
    static let cream = Color(red: 249 / 255, green: 249 / 255, blue: 220 / 255)

    /// `#8A7E72` — secondary text, borders, de-emphasized chrome.
    static let taupe = Color(red: 138 / 255, green: 126 / 255, blue: 114 / 255)

    // MARK: Derived (gradients & highlights)

    /// Lighter burgundy for gradient high-lights.
    static let burgundyLight = Color(red: 166 / 255, green: 65 / 255, blue: 92 / 255)

    /// Deeper burgundy for gradient ends / pressed affordances.
    static let burgundyDeep = Color(red: 92 / 255, green: 0 / 255, blue: 23 / 255)

    /// Warm off-white between cream and taupe (cards, chips).
    static let creamWarm = Color(red: 0.94, green: 0.92, blue: 0.86)

    // MARK: Legacy aliases (map old teal/mint/purple to new palette)

    static let mint = cream
    static let mintDeep = creamWarm
    static let teal = taupe
    static let tealDark = burgundy
    static let peach = Color(red: 0.97, green: 0.94, blue: 0.90)
    static let peachDeep = burgundyLight
    static let purple = burgundy
    static let purpleLight = burgundyLight

    // MARK: Typography on cream / glass

    static let titleForeground = burgundy

    static let cardTitle = Color(red: 0.12, green: 0.10, blue: 0.09)

    /// Legacy taupe — prefer [captionOnGlass] on glass cards for readability.
    static let cardSubtitle = taupe

    /// Secondary text on cream / glass — darker than taupe for WCAG-friendly contrast.
    static let captionOnGlass = Color(red: 0.30, green: 0.26, blue: 0.23)

    /// Small caps / meta lines on tinted cards (e.g. next session).
    static let captionOnTintedCard = Color(red: 0.28, green: 0.24, blue: 0.22)

    static let accentPrimary = burgundy

    /// Frosted tile base — sits under material on cream gradients.
    static let cardBaseFill = cream.opacity(0.97)

    // MARK: Gradients

    static var backgroundGradient: LinearGradient {
        LinearGradient(
            colors: [
                cream,
                Color(red: 0.96, green: 0.95, blue: 0.88),
                Color(red: 0.93, green: 0.91, blue: 0.86),
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    static var primaryButtonGradient: LinearGradient {
        LinearGradient(
            colors: [burgundyLight, burgundyDeep],
            startPoint: .leading,
            endPoint: .trailing
        )
    }

    /// Next-session / highlight cards (warm cream + burgundy wash).
    static var sessionCardGradient: LinearGradient {
        LinearGradient(
            colors: [
                Color(red: 0.99, green: 0.96, blue: 0.94),
                Color(red: 0.95, green: 0.88, blue: 0.86),
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}
