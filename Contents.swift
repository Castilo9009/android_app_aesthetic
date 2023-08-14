enum DogError: ErrorType {
    case WetDog
    case FlatulentDog
}

class Dog {
    private var wet: Bool
    private var hadPeopleFood: Bool
    
    init(wet: Bool, hadPeopleFood: Bool) {
        self.wet = wet
        self.hadPeopleFood = hadPeopleFood
    }
    
    func getOnBed() throws {
        guard (!wet) else {
            throw DogError.WetDog
        }
        
        guard (!hadPeopleFood) else {
            throw DogError.FlatulentDog
        }
        
        print("The dog got on the bed.")
    }
}

let juniper = Dog(wet: false, hadPeopleFood: true)
let ava = Dog(wet: true, hadPeopleFood: false)
let spacey = Dog(wet: false, hadPeopleFood: false)

do {
    try ava.getOnBed()
} catch DogError.WetDog {
    print("Wet dog!")
} catch DogError.FlatulentDog {
    print("Stinky dog!")
}

do {
    try juniper.getOnBed()
} catch let error as DogError {
    print("\(error)")
}

try! spacey.getOnBed()
