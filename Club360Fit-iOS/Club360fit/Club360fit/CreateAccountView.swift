import SwiftUI

/// Parity with Android `AuthScreen` (create account): metadata fields. New accounts are always `client`;
/// promoting a coach is done in Supabase (see Profile help when signed in as admin).
struct CreateAccountView: View {
    @Environment(Club360AuthSession.self) private var auth: Club360AuthSession
    @State private var name = ""
    @State private var age = ""
    @State private var height = ""
    @State private var weight = ""
    @State private var phone = ""
    @State private var email = ""
    @State private var password = ""
    @State private var medicalConditions = ""
    @State private var foodRestrictions = ""
    @State private var mealsPerDay = ""
    @State private var workoutFrequency = ""
    @State private var overallGoal = ""
    @State private var isBusy = false
    @State private var needsEmailConfirmation = false
    @State private var passwordVisible = false

    var body: some View {
        Form {
            Section {
                TextField("Email", text: $email)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .foregroundStyle(Club360Theme.cardTitle)
                PasswordFieldRow(
                    title: "Password",
                    text: $password,
                    isVisible: $passwordVisible,
                    isNewPassword: true
                )
            } header: {
                Text("Account")
                    .foregroundStyle(Club360Theme.cardTitle)
            }

            Section {
                TextField("Name", text: $name)
                    .foregroundStyle(Club360Theme.cardTitle)
                TextField("Age", text: $age)
                    .keyboardType(.numberPad)
                    .foregroundStyle(Club360Theme.cardTitle)
                TextField("Height (ft / in)", text: $height)
                    .foregroundStyle(Club360Theme.cardTitle)
                TextField("Weight (lbs)", text: $weight)
                    .foregroundStyle(Club360Theme.cardTitle)
                TextField("Phone", text: $phone)
                    .keyboardType(.phonePad)
                    .foregroundStyle(Club360Theme.cardTitle)
            } header: {
                Text("Profile")
                    .foregroundStyle(Club360Theme.cardTitle)
            }

            Section {
                TextField("Medical conditions", text: $medicalConditions, axis: .vertical)
                    .lineLimit(2 ... 4)
                    .foregroundStyle(Club360Theme.cardTitle)
                TextField("Food restrictions", text: $foodRestrictions, axis: .vertical)
                    .lineLimit(2 ... 4)
                    .foregroundStyle(Club360Theme.cardTitle)
                TextField("Meals per day", text: $mealsPerDay)
                    .foregroundStyle(Club360Theme.cardTitle)
                TextField("Workout frequency", text: $workoutFrequency)
                    .foregroundStyle(Club360Theme.cardTitle)
                TextField("Overall goal", text: $overallGoal, axis: .vertical)
                    .lineLimit(2 ... 4)
                    .foregroundStyle(Club360Theme.cardTitle)
            } header: {
                Text("Health & goals")
                    .foregroundStyle(Club360Theme.cardTitle)
            }

            if needsEmailConfirmation {
                Section {
                    Text("Check your email to confirm your account, then sign in.")
                        .font(.footnote)
                        .foregroundStyle(Club360Theme.captionOnGlass)
                }
            }
            if let err = auth.errorMessage {
                Section {
                    Text(err)
                        .foregroundStyle(.red)
                        .font(.footnote)
                }
            }
            Section {
                Button {
                    Task {
                        isBusy = true
                        needsEmailConfirmation = false
                        let ok = await auth.signUp(
                            email: email,
                            password: password,
                            name: name,
                            age: age,
                            height: height,
                            weight: weight,
                            phone: phone,
                            medicalConditions: medicalConditions,
                            foodRestrictions: foodRestrictions,
                            mealsPerDay: mealsPerDay,
                            workoutFrequency: workoutFrequency,
                            overallGoal: overallGoal
                        )
                        if !ok, auth.errorMessage == nil {
                            needsEmailConfirmation = true
                        }
                        isBusy = false
                    }
                } label: {
                    if isBusy {
                        HStack {
                            ProgressView()
                                .tint(.white)
                            Text("Creating account…")
                        }
                    } else {
                        Text("Create account")
                    }
                }
                .buttonStyle(Club360PrimaryGradientButtonStyle())
                .listRowBackground(Color.clear)
                .disabled(isBusy || !canSubmit)
            }
            Section {
                NavigationLink("Sign in instead") {
                    SignInView()
                }
            }
        }
        .tint(Club360Theme.burgundy)
        .club360FormScreen()
        .preferredColorScheme(.light)
        .navigationTitle("Create account")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
    }

    private var canSubmit: Bool {
        !email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !password.isEmpty
    }
}

#Preview {
    NavigationStack {
        CreateAccountView()
            .environment(Club360AuthSession())
    }
}
